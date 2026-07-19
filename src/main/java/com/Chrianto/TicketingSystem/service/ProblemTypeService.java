package com.Chrianto.TicketingSystem.service;

import com.Chrianto.TicketingSystem.dto.request.ProblemTypeRequest;
import com.Chrianto.TicketingSystem.dto.response.ProblemTypeResponse;
import com.Chrianto.TicketingSystem.entity.ProblemType;
import com.Chrianto.TicketingSystem.repository.ProblemTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProblemTypeService {
    private final ProblemTypeRepository problemTypeRepository;

    public ProblemTypeResponse createProblemType(ProblemTypeRequest req){
        ProblemType problemType = new ProblemType();

        problemType.setName(req.getName());

        problemType = problemTypeRepository.save(problemType);

        return toResponse(problemType);
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
