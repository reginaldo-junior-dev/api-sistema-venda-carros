package com.reginaldo.apisistemavendacarros.service;

import com.reginaldo.apisistemavendacarros.dto.imagem.ImagemCarroResponse;
import com.reginaldo.apisistemavendacarros.repository.*;
import com.reginaldo.apisistemavendacarros.specification.CarroSpecification;
import com.reginaldo.apisistemavendacarros.dto.carro.CarroFiltro;
import com.reginaldo.apisistemavendacarros.dto.carro.CarroRequest;
import com.reginaldo.apisistemavendacarros.dto.carro.CarroResponse;
import com.reginaldo.apisistemavendacarros.entity.Carro;
import com.reginaldo.apisistemavendacarros.entity.Categoria;
import com.reginaldo.apisistemavendacarros.entity.Cor;
import com.reginaldo.apisistemavendacarros.entity.Modelo;
import com.reginaldo.apisistemavendacarros.enums.StatusCarro;
import com.reginaldo.apisistemavendacarros.exception.CampoInvalidoException;
import com.reginaldo.apisistemavendacarros.exception.RecursoNaoEncontradoException;
import com.reginaldo.apisistemavendacarros.exception.ValorInvalidoException;
import com.reginaldo.apisistemavendacarros.mapper.CarroMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Year;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CarroService {

        private final CarroRepository carroRepository;
        private final ImagemCarroRepository imagemCarroRepository;
        private final CarroMapper mapper;
        private final ModeloRepository modeloRepository;
        private final CategoriaRepository categoriaRepository;
        private final CorRepository corRepository;
        private final S3Service s3Service;
        private final CompraRepository compraRepository;
        private final FavoritoRepository favoritoRepository;
        private final InteresseCarroRepository interesseCarroRepository;

        public CarroResponse cadastro (CarroRequest request) {
            validarAnos(request);

            Modelo modelo = modeloRepository.findById(request.modeloId()).orElseThrow(() ->
                    new RecursoNaoEncontradoException("Modelo não encontrado"));

            Categoria categoria = categoriaRepository.findById(request.categoriaId()).orElseThrow(() ->
                    new RecursoNaoEncontradoException("Categoria não encontrada"));

            Cor cor = corRepository.findById(request.corId()).orElseThrow(() ->
                    new RecursoNaoEncontradoException("Cor não encontrada"));

            Carro carro = mapper.toEntity(request);
            carro.setStatus(StatusCarro.DISPONIVEL);
            carro.setModelo(modelo);
            carro.setCategoria(categoria);
            carro.setCor(cor);

            carroRepository.save(carro);

            return mapper.toResponse(carro);
        }

        private CarroResponse montarResponse(Carro carro) {
            CarroResponse response = mapper.toResponse(carro);

            List<ImagemCarroResponse> imagens = response.imagens().stream()
                    .map(imagem -> new ImagemCarroResponse(
                            imagem.id(),
                            imagem.chaveArquivo(),
                            s3Service.gerarUrl(imagem.chaveArquivo()),
                            imagem.ordem(),
                            imagem.principal(),
                            imagem.carroId()
                    ))
                    .toList();

            return new CarroResponse(
                    response.id(),
                    response.nome(),
                    response.preco(),
                    response.descricao(),
                    response.anoFabricacao(),
                    response.anoModelo(),
                    response.quilometragem(),
                    response.condicao(),
                    response.combustivel(),
                    response.cambio(),
                    response.status(),
                    imagens,
                    response.modeloId(),
                    response.categoriaId(),
                    response.corId()
        );
    }

        public Page<CarroResponse> listar(CarroFiltro filtro, Pageable pageable) {
            Specification<Carro> specification = CarroSpecification.filtrar(filtro);

            Page<Carro> carros = carroRepository.findAll(specification, pageable);

            return carros.map(this::montarResponse);
    }

        public CarroResponse buscarPorId(UUID id) {
            Carro carro = carroRepository.findById(id).orElseThrow(() ->
                    new RecursoNaoEncontradoException("Carro não encontrado"));

            return montarResponse(carro);
    }

        public CarroResponse atualizar (UUID id, CarroRequest request) {
            Carro carro = carroRepository.findById(id).orElseThrow(() ->
                    new RecursoNaoEncontradoException("Carro não encontrado"));

            // A compra aponta para o carro: alterar um vendido mudaria o histórico da venda
            if (carro.getStatus() == StatusCarro.VENDIDO) {
                throw new ValorInvalidoException("Carro vendido não pode ser alterado");
            }

            validarAnos(request);

            Modelo modelo = modeloRepository.findById(request.modeloId()).orElseThrow(() ->
                    new RecursoNaoEncontradoException("Modelo não encontrado"));

            Categoria categoria = categoriaRepository.findById(request.categoriaId()).orElseThrow(() ->
                    new RecursoNaoEncontradoException("Categoria não encontrada"));

            Cor cor = corRepository.findById(request.corId()).orElseThrow(() ->
                    new RecursoNaoEncontradoException("Cor não encontrada"));

            mapper.atualizar(request, carro);
            carro.setModelo(modelo);
            carro.setCategoria(categoria);
            carro.setCor(cor);

            carroRepository.save(carro);

            return montarResponse(carro);
        }

        @Transactional
        public void excluir(UUID id) {

            // Bloqueia o carro para nenhuma compra ser criada entre a verificação e a exclusão
            Carro carro = carroRepository.findByIdComBloqueio(id)
                    .orElseThrow(() ->
                            new RecursoNaoEncontradoException("Carro não encontrado"));

            // Compras são histórico financeiro e impedem a exclusão
            if (compraRepository.existsByCarroId(id)) {
                throw new ValorInvalidoException("Carro possui compras registradas e não pode ser excluído");
            }

            List<String> chavesArquivos = carro.getImagens().stream()
                    .map(imagem -> imagem.getChaveArquivo())
                    .toList();

            favoritoRepository.deleteByCarroId(id);
            interesseCarroRepository.deleteByCarroId(id);
            imagemCarroRepository.deleteAll(carro.getImagens());
            carroRepository.delete(carro);

            // Imagens do S3 só são apagadas depois do commit: se o banco falhar, nada é perdido
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    chavesArquivos.forEach(s3Service::excluir);
                }
            });
    }

        // O ano do modelo é o de fabricação ou o seguinte; fabricação no futuro não existe
        private void validarAnos (CarroRequest request) {
            if (request.anoFabricacao() > Year.now().getValue()) {
                throw new CampoInvalidoException("anoFabricacao", "Ano de fabricação não pode estar no futuro");
            }

            int diferenca = request.anoModelo() - request.anoFabricacao();
            if (diferenca < 0 || diferenca > 1) {
                throw new CampoInvalidoException("anoModelo", "Ano do modelo deve ser igual ao ano de fabricação ou o seguinte");
            }
        }

}
