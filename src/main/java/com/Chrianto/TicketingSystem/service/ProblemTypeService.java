package com.Chrianto.TicketingSystem.service;

import com.Chrianto.TicketingSystem.dto.request.ProblemTypeRequest;
import com.Chrianto.TicketingSystem.dto.response.ProblemTypeResponse;
import com.Chrianto.TicketingSystem.entity.ProblemType;
import com.Chrianto.TicketingSystem.exception.EntityNotFoundException;
import com.Chrianto.TicketingSystem.repository.ProblemTypeRepository;
import com.Chrianto.TicketingSystem.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProblemTypeService {
    private final ProblemTypeRepository problemTypeRepository;
    private final TicketRepository ticketRepository;

    public ProblemTypeResponse createProblemType(ProblemTypeRequest req){
        ProblemType problemType = new ProblemType();

        problemType.setName(req.getName());

        problemType = problemTypeRepository.save(problemType);

        return toResponse(problemType);
    }

    @Transactional
    public void deleteProblemType(Long problemTypeId) {
        if (!problemTypeRepository.existsById(problemTypeId)) {
            throw new EntityNotFoundException("ProblemType not found with id: " + problemTypeId);
        }

        ticketRepository.nullifyProblemType(problemTypeId);
        problemTypeRepository.deleteById(problemTypeId);
    }

    public List<ProblemTypeResponse> getAllProblemTypes(){
        return problemTypeRepository.findAll().
                stream().
                map(this::toResponse).
                toList();
    }

    private ProblemTypeResponse toResponse(ProblemType problemType) {
        return ProblemTypeResponse.builder()
                .id(problemType.getId())
                .name(problemType.getName())
                .build();
    }


}
