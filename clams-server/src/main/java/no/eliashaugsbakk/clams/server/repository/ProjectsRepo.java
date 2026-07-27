package no.eliashaugsbakk.clams.server.repository;

import java.util.List;
import no.eliashaugsbakk.clams.server.model.Project;

public interface ProjectsRepo {
  void addProject(Project post);
  boolean updateProject(Project project);
  boolean deleteProject(long id);

  List<Project> getAllProjects();
}
