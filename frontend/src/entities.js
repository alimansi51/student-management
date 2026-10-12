// Describes each entity's admin screen: which API endpoint it uses and which fields it has.
// Field types: text (default), email, url, textarea, date, number, file (upload from the device),
// filelink (link to open an uploaded file), ref (pick one item from another entity), multiref (pick many).
// readOnly fields are shown in the table but not in the form; formOnly fields are the opposite.
// format(value) customises how a value is shown.
// rowAction adds an extra button to each row that opens the given component.

import AssignmentSubmissions from './components/AssignmentSubmissions';

const studentName = (u) => u.fullName;
const courseField = { name: 'course', label: 'Course', type: 'ref', source: 'courses', show: (c) => c.title, required: true };

export const entities = [
  {
    key: 'users',
    title: 'Students',
    singular: 'Student',
    resetPassword: true,
    fields: [
      { name: 'firstName', label: 'First name', required: true },
      { name: 'lastName', label: 'Last name', required: true },
      { name: 'email', label: 'Email', type: 'email', required: true },
      { name: 'phone', label: 'Phone' },
      { name: 'username', label: 'Username', readOnly: true },
      {
        name: 'mustChangePassword',
        label: 'Password',
        readOnly: true,
        format: (v) => (v ? 'One-time (not changed yet)' : 'Set by student'),
      },
    ],
  },
  {
    key: 'courses',
    title: 'Courses',
    singular: 'Course',
    fields: [
      { name: 'title', label: 'Title', required: true },
      { name: 'description', label: 'Description', type: 'textarea' },
      { name: 'students', label: 'Enrolled students', type: 'multiref', source: 'users', show: studentName },
    ],
  },
  {
    key: 'videos',
    title: 'Videos',
    singular: 'Video',
    jsonPart: 'video', // name of the JSON part in the upload (see VideoController)
    fields: [
      { name: 'title', label: 'Title', required: true },
      { name: 'file', label: 'Video file', type: 'file', accept: 'video/*', required: true, formOnly: true },
      { name: 'originalFileName', label: 'Video', type: 'filelink', urlField: 'streamUrl', icon: '▶', readOnly: true },
      courseField,
    ],
  },
  {
    key: 'materials',
    title: 'Lectures & Materials',
    singular: 'Material',
    jsonPart: 'material', // name of the JSON part in the upload (see MaterialController)
    fields: [
      { name: 'title', label: 'Title', required: true },
      { name: 'description', label: 'Description', type: 'textarea' },
      {
        name: 'file',
        label: 'File (PDF, slides, document…)',
        type: 'file',
        accept: '.pdf,.ppt,.pptx,.doc,.docx,.xls,.xlsx,.txt,.zip,.png,.jpg,.jpeg',
        required: true,
        formOnly: true,
      },
      { name: 'originalFileName', label: 'File', type: 'filelink', urlField: 'fileUrl', icon: '📄', readOnly: true },
      courseField,
    ],
  },
  {
    key: 'assignments',
    title: 'Assignments',
    singular: 'Assignment',
    // students' PDFs are seen and graded from here
    rowAction: { label: 'Submissions', component: AssignmentSubmissions },
    fields: [
      { name: 'title', label: 'Title', required: true },
      { name: 'description', label: 'Description', type: 'textarea' },
      { name: 'dueDate', label: 'Due date', type: 'date' },
      courseField,
    ],
  },
];
