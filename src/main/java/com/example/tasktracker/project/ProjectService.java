package com.example.tasktracker.project;

import com.example.tasktracker.project.dto.ProjectRequest;
import com.example.tasktracker.project.dto.ProjectResponse;
import com.example.tasktracker.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ProjectMapper projectMapper;

    @Transactional
    public ProjectResponse create(ProjectRequest request, Long ownerId) {
        Project project = new Project(
                request.name(),
                request.description(),
                userRepository.getReferenceById(ownerId)
        );

        return projectMapper.toResponse(projectRepository.save(project));
    }

    public ProjectResponse getById(Long id) {
        return projectMapper.toResponse(findOrThrow(id));
    }

    public Page<ProjectResponse> getAll(Pageable pageable) {
        return projectRepository.findAll(pageable).map(projectMapper::toResponse);
    }

    @Transactional
    public ProjectResponse update(Long id, ProjectRequest request) {
        Project project = findOrThrow(id);
        project.setName(request.name());
        project.setDescription(request.description());

        return projectMapper.toResponse(project);
    }

    @Transactional
    public void delete(Long id) {
        projectRepository.delete(findOrThrow(id));
    }

    private Project findOrThrow(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Project not found: " + id));
    }


}
