package no.eliashaugsbakk.clams.server.controller;

import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import java.util.List;
import java.util.Map;
import no.eliashaugsbakk.clams.server.model.Project;
import no.eliashaugsbakk.clams.server.repository.ProjectsRepo;

public class ProjectController {
  private final ProjectsRepo projectsRepo;

  public ProjectController(ProjectsRepo projectsRepo) {
    this.projectsRepo = projectsRepo;
  }

  public void handleGetProjects(Context ctx) {
    List<Project> allProjects = projectsRepo.getAllProjects();
    ctx.render("templates/projects.html", Map.of(
        "page_title", "Blog",
        "page_css", "projects",
        "projects", allProjects));
  }

  public void handleGetProjectsApi(Context ctx) {
    ctx.json(projectsRepo.getAllProjects());
  }

  public void handlePostProject(Context ctx) {
    Project newProject = ctx.bodyAsClass(Project.class);
    projectsRepo.addProject(newProject);
    ctx.status(HttpStatus.CREATED);
  }

  public void handlePutProject(Context ctx) {
    long id = parseId(ctx);
    Project updateProject = ctx.bodyAsClass(Project.class);

    Project projectToUpdate = new Project(
        id,
        updateProject.name(),
        updateProject.readMoreUrl(),
        updateProject.gitUrl(),
        updateProject.gitHubUrl(),
        updateProject.description()
    );

    boolean updated = projectsRepo.updateProject(projectToUpdate);
    if (!updated) {
      ctx.status(HttpStatus.NOT_FOUND);
      return;
    }

    ctx.status(HttpStatus.NO_CONTENT);
  }

  public void handleDeleteProject(Context ctx) {
    long id = parseId(ctx);
    boolean deleted = projectsRepo.deleteProject(id);
    if (!deleted) {
      ctx.status(HttpStatus.NOT_FOUND);
      return;
    }
    ctx.status(HttpStatus.NO_CONTENT);
  }

  private long parseId(Context ctx) {
    try {
      return Long.parseLong(ctx.pathParam("id"));
    } catch (NumberFormatException e) {
      throw new BadRequestResponse("Invalid project ID format");
    }
  }
}
