# Clams HTTP API

> [!NOTE]
> This API reference was written by an LLM from the existing Java server
> implementation. Verify it against the server before treating it as a stable
> contract.

The API base URL is the server URL. Requests under `/api` require:

```http
Authorization: Bearer <authorization-token>
```

The unversioned `/api` namespace is the project's version-one API contract.
Compatible additions may be made within the `1.x` release line. Existing
endpoints and response fields should not be removed or change meaning without a
major-version decision.

API errors use this JSON shape:

```json
{
  "error": "Bad Request",
  "message": "Field 'title' is required.",
  "status": 400
}
```

Browser-facing routes render the site's HTML error page instead. The public
`/media` routes remain intentionally available without an API token so blog
images can be embedded in web pages.

Mutation payload limits are currently:

- post `title`: required, maximum 200 characters;
- post `content`: required, maximum 1,000,000 characters;
- post `summary`: optional, maximum 1,000 characters;
- project `name`: required, maximum 200 characters;
- project URLs: optional, maximum 2,000 characters;
- project `description`: optional, maximum 10,000 characters.

Invalid payloads return `400 Bad Request`; missing resources return `404 Not
Found`; unsupported image formats return `415 Unsupported Media Type`.

The server can set `site_url` in `~/.config/clams/clams.properties` to the
public origin used in generated links:

```properties
site_url=https://example.com
```

## Posts

### `GET /api/posts`

Returns post metadata, including the immutable numeric `id` and current SEO
`slug`, for authenticated CLI selection and management.

### `GET /api/posts/{id}`

Returns the full post for an authenticated numeric ID.

### `POST /api/posts`

Creates a post. JSON body:

```json
{
  "title": "Post title",
  "summary": "Short summary",
  "content": "Markdown content",
  "isPublished": true
}
```

Returns `201 Created` with the immutable post ID and generated slug:

```json
{
  "id": 42,
  "slug": "post-title"
}
```

### `PUT /api/posts/{id}`

Updates a post selected by immutable numeric ID. The slug is generated when
the post is created and is not used as the technical identity.

Returns `204 No Content` when the post exists and is updated.

### `DELETE /api/posts/{id}`

Deletes a post selected by immutable numeric ID.

Public post URLs use the format `/posts/{id}/{slug}`. The ID resolves the post;
the slug is a readable, SEO-oriented URL component. If the slug is stale but
the ID exists, the server redirects to the current canonical URL. Slug-only
post URLs are not supported.

## Projects

### `GET /api/projects`

Returns all projects, including their numeric IDs and `displayOrder`, sorted by
`displayOrder` ascending and then ID ascending.

### `POST /api/projects`

Creates a project. JSON fields are `name`, `readMoreUrl`, `gitUrl`,
`gitHubUrl`, `description`, and optional `displayOrder`. Lower display order
values appear first. If omitted, a new project is appended after existing
projects.
`displayOrder` must be zero or greater.

Returns `201 Created` with an empty response body. `name` is required; the
other fields are optional.

### `PUT /api/projects/{id}`

Updates a project using the same fields as creation. Omitting `displayOrder`
preserves the current order.

Returns `204 No Content` when the project exists and is updated.

### `DELETE /api/projects/{id}`

Deletes a project.

Returns `204 No Content` when the project exists and is deleted.

## Images

### `POST /api/media`

Uploads a JPEG using a multipart form field named `image`. The server accepts
images no larger than 2000x2000 pixels. Files larger than 5 MiB are accepted
but return an `X-Clams-Warning` response header recommending compression.
Files larger than 20 MiB are rejected with `413 Payload Too Large`.

Successful response (`201`):

```json
{
  "uuid": "generated-image-uuid",
  "url": "/media/generated-image-uuid",
  "originalFilename": "diagram.jpeg",
  "contentType": "image/jpeg",
  "timeUploaded": "2026-09-10T12:18:25Z"
}
```

The upload response includes the public browser URL and the metadata stored for
the image. `contentType` may be `null` when the multipart client omits it.

### `GET /api/media`

Returns image metadata for authenticated image overview and reuse.

### `DELETE /api/media/{uuid}`

Deletes image metadata and the stored image file when the image is not
referenced by any stored post. The server returns `409 Conflict` when deletion
would leave a post with a broken image reference.

Returns `204 No Content` when the image exists and is deleted.

### `GET /media/{uuid}`

Returns the JPEG image without requiring an API token. This route is intended
for Markdown embedded in public blog posts.

## RSS

### `GET /rss.xml`

Returns a public RSS 2.0 summary feed containing only published posts. Each
item includes the title, summary, publication date, and a link to the full
post on the website. Full post content is intentionally not included.

The response uses `Content-Type: application/rss+xml` and
`Cache-Control: public, max-age=900`. When `site_url` is not configured, the
server derives absolute links from the request URL.
