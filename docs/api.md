# Clams HTTP API

> [!NOTE]
> This API reference was written by an LLM from the existing Java server
> implementation. Verify it against the server before treating it as a stable
> contract.

The API base URL is the server URL. Requests under `/api` require:

```http
Authorization: Bearer <authorization-token>
```

The server can set `site_url` in `~/.config/clams/clams.properties` to the
public origin used in generated links:

```properties
site_url=https://example.com
```

## Posts

### `GET /api/posts`

Returns post metadata for authenticated CLI selection and management.

### `GET /api/posts/{slug}`

Returns the full post for an authenticated slug.

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

### `PUT /api/posts/{slug}`

Updates a post using the same JSON body as creation.

### `DELETE /api/posts/{slug}`

Deletes a post.

## Projects

### `GET /api/projects`

Returns all projects, including their numeric IDs.

### `POST /api/projects`

Creates a project. JSON fields are `name`, `readMoreUrl`, `gitUrl`,
`gitHubUrl`, and `description`.

### `PUT /api/projects/{id}`

Updates a project using the same fields as creation.

### `DELETE /api/projects/{id}`

Deletes a project.

## Images

### `POST /api/media`

Uploads a JPEG using a multipart form field named `image`. The server accepts
images no larger than 2000x2000 pixels.

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

Deletes image metadata and the stored image file.

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
