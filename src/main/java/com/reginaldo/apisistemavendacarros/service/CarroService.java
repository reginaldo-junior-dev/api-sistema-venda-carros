package com.reginaldo.apisistemavendacarros.service;

import com.reginaldo.apisistemavendacarros.Specification.CarroSpecification;
import com.reginaldo.apisistemavendacarros.dto.CarroFiltro;
import com.reginaldo.apisistemavendacarros.dto.CarroRequest;
import com.reginaldo.apisistemavendacarros.dto.CarroResponse;
import com.reginaldo.apisistemavendacarros.entity.Carro;
import com.reginaldo.apisistemavendacarros.entity.Categoria;
import com.reginaldo.apisistemavendacarros.entity.Cor;
import com.reginaldo.apisistemavendacarros.entity.Modelo;
import com.reginaldo.apisistemavendacarros.enums.StatusCarro;
import com.reginaldo.apisistemavendacarros.exception.RecursoNaoEncontradoException;
import com.reginaldo.apisistemavendacarros.mapper.CarroMapper;
import com.reginaldo.apisistemavendacarros.repository.CarroRepository;
import com.reginaldo.apisistemavendacarros.repository.CategoriaRepository;
import com.reginaldo.apisistemavendacarros.repository.CorRepository;
import com.reginaldo.apisistemavendacarros.repository.ModeloRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CarroService {

        private final CarroRepository carroRepository;
        private final CarroMapper mapper;
        private final ModeloRepository modeloRepository;
        private final CategoriaRepository categoriaRepository;
        private final CorRepository corRepository;

        public CarroResponse cadastro (CarroRequest request) {
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

        public Page<CarroResponse> listar (CarroFiltro filtro, Pageable pageable) {
            Specification<Carro> specification = CarroSpecification.filtrar(filtro);

            Page<Carro> carros = carroRepository.findAll(specification, pageable);

            return carros.map(mapper::toResponse);
        }

        public CarroResponse buscarPorId (UUID id) {
            Carro carro = carroRepository.findById(id).orElseThrow(() ->
                    new RecursoNaoEncontradoException("Carro não encontrado"));

            return mapper.toResponse(carro);
        }

        public CarroResponse atualizar (UUID id, CarroRequest request) {
            Carro carro = carroRepository.findById(id).orElseThrow(() ->
                    new RecursoNaoEncontradoException("Carro não encontrado"));

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

            return mapper.toResponse(carro);
        }

        public void excluir (UUID id) {
            carroRepository.findById(id).orElseThrow(() ->
                    new RecursoNaoEncontradoException("Carro não encontrado"));

            carroRepository.deleteById(id);
        }

}
