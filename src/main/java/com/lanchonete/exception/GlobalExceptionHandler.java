package com.lanchonete.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;


/**
 * Tratamento centralizado de exceções da API.
 *
 * Utiliza o padrão RFC 9457 (Problem Details for HTTP APIs) através
 * do ProblemDetail do Spring 6+, retornando respostas JSON padronizadas
 * e amigáveis para o consumidor da API.
 *
 * Hierarquia de respostas:
 * - 400: Erros de validação de campos (@Valid)
 * - 404: Recurso não encontrado
 * - 422: Operação de negócio inválida
 * - 500: Erros inesperados
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {


    /**
     * Trata erros de validação do Bean Validation (@Valid / @NotBlank .)
     * Retorna um mapa detalhando qual campo falhou e o motivo.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationErrors(MethodArgumentNotValidException ex){
        log.warn(" ❌ Erro de validalção: {}" , ex.getMessage());

        Map<String, String> erros = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String campo = ((FieldError) error).getField();
            String mensagem = error.getDefaultMessage();
            erros.put(campo, mensagem);
        });

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Erro de Validação ");
        problem.setDetail("Um ou mais campos possuem valores inválidos. ");
        problem.setType(URI.create("https://lanchonete.api/erros/validacao"));
        problem.setProperty("erros" , erros);
        problem.setProperty("timestamp" , Instant.now());

        return problem;
    }

    /**
     * Trata erros de recurso não encontrado (404).
     * Disparado quando produto ou pedido não existe no banco.
     */
    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ProblemDetail handleRecursoNaoEncontrado(RecursoNaoEncontradoException ex){
        log.warn(" ❌ Recurso não encontrado: {}" , ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problem.setTitle("Recurso Não Encontrado");
        problem.setDetail(ex.getMessage());
        problem.setType(URI.create("https://lanchonete.api/erros/nao-encontrado"));
        problem.setProperty("timestamp" , Instant.now());

        return problem;
    }

    /**
     * Trata erros de regras de negócio (422 Unprocessable Entity).
     * Ex: pedido de produto indisponível, transição de status inválida.
     */
    @ExceptionHandler(OperacaoInvalidaException.class)
    public ProblemDetail handleOperacaoInvalida(OperacaoInvalidaException ex){
        log.warn("❌ Operação Inválida: {}" , ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.UNPROCESSABLE_ENTITY);
        problem.setTitle(" Operação Inválida");
        problem.setDetail(ex.getMessage());
        problem.setType(URI.create("https://lanchonete.api/erros/operacao-invalida"));
        problem.setProperty("timestamp" , Instant.now());

        return problem;
    }

    /**
     * Captura qualquer exceção não tratada (500).
     * Evita vazar stack traces para o cliente.
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleErroGenerico(Exception ex){
        log.error("😶 Erro inesperado na API: {}" , ex.getMessage(), ex);

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        problem.setTitle("Erro Interno");
        problem.setDetail("Ocorreu um erro inesperado. Por favor , tente novamente mais tarde. ");
        problem.setType(URI.create("https://lanchonete.api/erros/interno"));
        problem.setProperty("timestamp" , Instant.now());

        return problem;
    }


}
