# Student Management System

A simple school system with two kinds of login:

- **Admin / teacher** (one account): manages students, courses, videos, **lectures & materials** and assignments. Each assignment has a **Submissions** button that lists every student in the course with their submitted PDF and a box to grade it. The dashboard lists work waiting for a grade.
- **Students**: see their courses as clickable cards. Opening a course shows tabs for that course's **Videos**, **Materials**, **Assignments** (hand in a PDF here) and **Grades**. Students can also change their password under **My account**.

The backend is Spring Boot and the frontend is React.

## How it uses MVC

| Layer      | Where                                         | What it does                                  |
|------------|-----------------------------------------------|-----------------------------------------------|
| Model      | `backend/.../model` + `repository` (+ `dto`)  | JPA entities and database access              |
| Controller | `backend/.../controller` (+ `service`)        | REST endpoints; services hold business logic  |
| View       | `frontend/` (React)                           | The admin and student screens                 |

## Entities

- **User**: a student (first name, last name, email, phone, plus a generated username and password)
- **Course**: title, description, enrolled students (many-to-many with User)
- **Video**: title and an uploaded video file, belongs to a Course
- **Material**: a lecture, slides, notes or any other file for a Course, with a title and description but no due date
- **Assignment**: title, description, due date, belongs to a Course
- **Submission**: a student's PDF answer to an assignment, with an optional grade

## Student logins

When the admin adds a student, a login is created automatically:

- **Username:** `firstname.lastname`, in lowercase, e.g. `john.smith`. If that's taken, it becomes `john.smith2`, then `john.smith3`, and so on.
- **Password:** an 8-character **one-time password**. It is shown to the admin **only once**, right after the student is created. Passwords are stored as BCrypt hashes.

The first time a student logs in with the one-time password, they must choose their own password before they can use the portal. The backend enforces this as well.

If a student loses their password, the admin clicks **Reset password** on the Students page. A message shows the student's new one-time password, and the student must change it again at their next login. The Students table shows whether each student is still on a one-time password. Students can also change their password any time under **My account**.

A student can only see courses they are enrolled in, plus those courses' videos, materials and assignments.

## Files: videos, materials and submissions

All uploads are saved under `backend/uploads/` under random names. Videos go in `videos/`, lectures and materials in `materials/`, and student PDFs in `submissions/`. The maximum size is 2 GB; change it in `application.properties`.

- **Videos:** the admin picks a video file and the form shows an upload progress bar. Students watch videos in a player. Browsers play **MP4 (H.264)** and **WebM** best.
- **Lectures & materials:** the admin uploads any file (PDF, slides, Word, Excel, images, zip…) to a course. Students see them in the course's **Materials** tab. Clicking one always downloads the file, PDFs included.
- **Submissions:** students hand in a **PDF**; other file types are refused. They can replace it until the teacher grades it. The teacher opens it from the assignment's **Submissions** window.
- **Editing and deleting:** editing a video or material without choosing a new file keeps the current one. Deleting a video, material, assignment, student or course also deletes its files.
- **Signed links:** a video player or a plain link can't send a login, so every file link is **signed**. It only works for that one file and expires after 6 hours. Only people allowed to see the item receive the link.

## Requirements

- JDK 17 or newer
- Maven, or an IDE like IntelliJ IDEA that comes with it
- Node.js 18 or newer

## Run it

**1. Backend** (port 8080)

Open the `backend` folder in IntelliJ and run `StudentManagementApplication`. Or run:

```bash
cd backend
mvn spring-boot:run
```

Data is saved in an H2 file database at `backend/data/`.

**2. Frontend** (port 3000)

```bash
cd frontend
npm install
npm run dev
```

Open http://localhost:3000. The admin login is `admin` / `admin123`; change it in `backend/src/main/resources/application.properties`.

## API

All endpoints need HTTP Basic auth, except the signed file links.

Admin only: `GET/POST /api/{entity}`, `GET/PUT/DELETE /api/{entity}/{id}`, where `{entity}` is one of `users`, `courses`, `videos`, `materials`, `assignments`. Also:

- `POST /api/users/{id}/reset-password`
- `GET /api/submissions` and `DELETE /api/submissions/{id}`
- `PUT /api/submissions/{id}/grade` with `{ "grade": 85 }`, or `null` to clear it

Videos and materials are created and updated with a multipart request instead of JSON. It has a `video` (or `material`) part with the JSON details, e.g. `{"title": "...", "course": {"id": 1}}`, and a `file` part with the file itself.

Signed file links (from `streamUrl` / `fileUrl`):

- `GET /api/videos/{id}/stream`
- `GET /api/materials/{id}/file`
- `GET /api/submissions/{id}/file`

Student only:

| Method | Path                       | What                                                    |
|--------|----------------------------|---------------------------------------------------------|
| GET    | `/api/student/me`          | My profile                                              |
| GET    | `/api/student/courses`     | My courses                                              |
| GET    | `/api/student/videos`      | Videos of my courses                                    |
| GET    | `/api/student/materials`   | Lectures & materials of my courses                      |
| GET    | `/api/student/assignments` | Assignments, with my submission and grade               |
| POST   | `/api/student/submissions` | Multipart form: `assignmentId` + `file` (a PDF)         |
| PUT    | `/api/student/password`    | `{ "currentPassword": "…", "newPassword": "…" }`        |

Both roles: `GET /api/auth/me` returns who is logged in (`role` and `name`).
