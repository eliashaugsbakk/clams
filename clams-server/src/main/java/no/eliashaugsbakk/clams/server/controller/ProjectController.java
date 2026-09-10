package no.eliashaugsbakk.clams.server.controller;

import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import java.util.List;
import java.util.Map;
import no.eliashaugsbakk.clams.server.model.Project;
import no.eliashaugsbakk.clams.server.repository.ProjectsRepo;
import no.eliashaugsbakk.clams.server.utils.ApiValidation;
import no.eliashaugsbakk.clams.server.utils.ErrorResponses;

// BEGIN LLM EDIT: Added stable validation constraints for project API payloads.
/**
 * Project page and API controller.
 *
 * <p>Disclaimer: The project API validation changes in this file were written by an LLM.</p>
 */
// END LLM EDIT
public class ProjectController {
  private final ProjectsRepo projectsRepo;

  public ProjectController(ProjectsRepo projectsRepo) {
    this.projectsRepo = projectsRepo;
  }

  public void handleGetProjects(Context ctx) {
    List<Project> allProjects = projectsRepo.getAllProjects();
    ctx.render("templates/projects.html", Map.of(
        "page_title", "Prosjekter",
        "page_css", "projects",
        "projects", allProjects));
  }

  public void handleGetProjectsApi(Context ctx) {
    ctx.json(projectsRepo.getAllProjects());
  }

  public void handlePostProject(Context ctx) {
    Project newProject = ctx.bodyAsClass(Project.class);
    validateProject(newProject);
    projectsRepo.addProject(newProject);
    ctx.status(HttpStatus.CREATED);
  }

  public void handlePutProject(Context ctx) {
    long id = parseId(ctx);
    Project updateProject = ctx.bodyAsClass(Project.class);
    validateProject(updateProject);

    Project projectToUpdate = new Project(
        id,
        updateProject.name(),
        updateProject.readMoreUrl(),
        updateProject.gitUrl(),
        updateProject.gitHubUrl(),
        updateProject.description(),
        updateProject.displayOrder()
    );

    boolean updated = projectsRepo.updateProject(projectToUpdate);
    if (!updated) {
      ErrorResponses.notFound(ctx, "Project not found.");
      return;
    }

    ctx.status(HttpStatus.NO_CONTENT);
  }

  public void handleDeleteProject(Context ctx) {
    long id = parseId(ctx);
    boolean deleted = projectsRepo.deleteProject(id);
    if (!deleted) {
      ErrorResponses.notFound(ctx, "Project not found.");
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

  // BEGIN LLM EDIT: Apply consistent size constraints while preserving optional project fields.
  private void validateProject(Project project) {
    ApiValidation.requiredText("name", project.name(), 200);
    ApiValidation.optionalText("readMoreUrl", project.readMoreUrl(), 2_000);
    ApiValidation.optionalText("gitUrl", project.gitUrl(), 2_000);
    ApiValidation.optionalText("gitHubUrl", project.gitHubUrl(), 2_000);
    ApiValidation.optionalText("description", project.description(), 10_000);
  }
  // END LLM EDIT
}
