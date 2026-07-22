package no.eliashaugsbakk.clams.server.controller;

import io.javalin.http.Context;
import java.util.List;
import java.util.Map;
import no.eliashaugsbakk.clams.server.model.Project;
import no.eliashaugsbakk.clams.server.repository.ProjectsRepo;

public class ProjectsController {
  private final ProjectsRepo projectsRepo;

  public ProjectsController(ProjectsRepo projectsRepo) {
    this.projectsRepo = projectsRepo;
  }

  public void handleGetProjects(Context ctx) {
    List<Project> allProjects = projectsRepo.getAllProjects();
    ctx.render("templates/projects.html",
        Map.of(
            "page_title", "Blog",
            "page_css", "projects",
            "projects", allProjects
        ));
  }

  public void handlePostProject(Context ctx) {
    Project newProject = ctx.bodyAsClass(Project.class);
    projectsRepo.addProject(newProject);
  }

  public void handlePutProject(Context ctx) {
    Project updateProject = ctx.bodyAsClass(Project.class);
  projectsRepo.updateProject(Long.parseLong(ctx.pathParam("id")), updateProject);
  }

  public void handleDeleteProject(Context ctx) {
    projectsRepo.deleteProject(Long.parseLong(ctx.pathParam("id")));
  }

  public void handleGetProjectsApi(Context ctx) {
    List<Project> allProjects = projectsRepo.getAllProjects();
    ctx.json(allProjects);
  }
}
