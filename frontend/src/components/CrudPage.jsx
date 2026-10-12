import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { api, upload } from '../api';

// One page that lists, creates, edits and deletes items of any entity described in entities.js.

const editable = (f) => !f.readOnly;
const inTable = (f) => !f.formOnly;
const isFile = (f) => f.type === 'file';

// item from the API -> values for the form inputs
function toForm(fields, item = {}) {
  const form = {};
  for (const f of fields.filter(editable)) {
    const v = item[f.name];
    if (f.type === 'ref') form[f.name] = v?.id ?? '';
    else if (f.type === 'multiref') form[f.name] = (v ?? []).map((x) => x.id);
    else if (isFile(f)) form[f.name] = null; // a File object once one is chosen
    else form[f.name] = v ?? '';
  }
  return form;
}

// form values -> JSON body for the API (relations are sent as { id }; files are sent separately)
function toPayload(fields, form) {
  const body = {};
  for (const f of fields.filter(editable)) {
    const v = form[f.name];
    if (isFile(f)) continue;
    if (f.type === 'ref') body[f.name] = v === '' ? null : { id: Number(v) };
    else if (f.type === 'multiref') body[f.name] = v.map((id) => ({ id }));
    else if (f.type === 'number') body[f.name] = v === '' ? null : Number(v);
    else body[f.name] = v === '' ? null : v;
  }
  return body;
}

function display(f, value, item) {
  if (f.format) return f.format(value);
  if (value == null || value === '' || (Array.isArray(value) && value.length === 0)) return '—';
  switch (f.type) {
    case 'ref':
      return f.show(value);
    case 'multiref':
      return value.map(f.show).join(', ');
    case 'url':
      return <a href={value} target="_blank" rel="noreferrer">Open</a>;
    case 'filelink': // an uploaded file; f.urlField names the item's signed link to open it
      return item[f.urlField]
        ? <a href={item[f.urlField]} target="_blank" rel="noreferrer">{f.icon} {value}</a>
        : value;
    case 'textarea':
      return value.length > 60 ? value.slice(0, 60) + '…' : value;
    case 'datetime':
      return value.replace('T', ' ').slice(0, 16);
    default:
      return String(value);
  }
}

const formatSize = (bytes) =>
  bytes > 1024 * 1024 * 1024
    ? `${(bytes / 1024 / 1024 / 1024).toFixed(1)} GB`
    : `${(bytes / 1024 / 1024).toFixed(1)} MB`;

export default function CrudPage({ entity }) {
  const { key, title, singular, fields } = entity;
  const [items, setItems] = useState([]);
  const [options, setOptions] = useState({}); // items of related entities, for dropdowns
  const [form, setForm] = useState(null); // null = form closed
  const [editingId, setEditingId] = useState(null);
  const [search, setSearch] = useState('');
  const [error, setError] = useState('');
  const [progress, setProgress] = useState(null); // upload progress in %, null when not uploading
  // { kind: 'created' | 'reset', name, username, password } to show the admin once
  const [credentials, setCredentials] = useState(null);
  // { title, message, button, danger, action } for the "are you sure?" dialog
  const [confirmBox, setConfirmBox] = useState(null);
  // the row whose extra action (entity.rowAction) is open, e.g. an assignment's submissions
  const [actionItem, setActionItem] = useState(null);
  const [searchParams, setSearchParams] = useSearchParams();
  const RowAction = entity.rowAction?.component;

  const fileFields = fields.filter(isFile);
  const tableFields = fields.filter(inTable);
  const uploading = progress !== null;

  const load = async () => {
    try {
      const list = await api(`/${key}`);
      setItems(list);
      // ?open=<id> in the address opens that row's action straight away (used by the dashboard)
      const openId = Number(searchParams.get('open'));
      if (RowAction && openId) setActionItem(list.find((it) => it.id === openId) ?? null);
      const sources = [...new Set(fields.filter((f) => f.source).map((f) => f.source))];
      const opts = {};
      for (const s of sources) opts[s] = await api(`/${s}`);
      setOptions(opts);
    } catch (e) {
      setError(e.message);
    }
  };

  useEffect(() => {
    load();
  }, []);

  const closeAction = () => {
    setActionItem(null);
    if (searchParams.has('open')) setSearchParams({}, { replace: true });
  };

  const openForm = (item) => {
    setError('');
    setEditingId(item?.id ?? null);
    setForm(toForm(fields, item));
  };

  const closeForm = () => {
    if (!uploading) setForm(null);
  };

  // Entities with a file are sent as multipart: the JSON details in one part, each file in its own part
  const sendWithFiles = (path, method, body) => {
    const formData = new FormData();
    formData.append(entity.jsonPart, new Blob([JSON.stringify(body)], { type: 'application/json' }));
    for (const f of fileFields) {
      if (form[f.name]) formData.append(f.name, form[f.name]);
    }
    setProgress(0);
    return upload(path, { method, formData, onProgress: setProgress });
  };

  const save = async (e) => {
    e.preventDefault();
    setError('');
    try {
      const body = toPayload(fields, form);
      const path = editingId ? `/${key}/${editingId}` : `/${key}`;
      const method = editingId ? 'PUT' : 'POST';
      const result = fileFields.length
        ? await sendWithFiles(path, method, body)
        : await api(path, { method, body });

      // new students come back with their generated login
      if (!editingId && result?.initialPassword) {
        setCredentials({
          kind: 'created',
          name: result.fullName,
          username: result.username,
          password: result.initialPassword,
        });
      }
      setForm(null);
      load();
    } catch (err) {
      setError(err.message);
    } finally {
      setProgress(null);
    }
  };

  const remove = (item) =>
    setConfirmBox({
      title: `Delete ${singular.toLowerCase()}?`,
      message: 'This cannot be undone.',
      button: 'Delete',
      danger: true,
      action: async () => {
        await api(`/${key}/${item.id}`, { method: 'DELETE' });
        load();
      },
    });

  const resetPassword = (item) =>
    setConfirmBox({
      title: 'Reset password?',
      message: `${item.fullName} will get a new one-time password and their current password will stop working.`,
      button: 'Reset password',
      action: async () => {
        const result = await api(`/${key}/${item.id}/reset-password`, { method: 'POST' });
        setCredentials({ kind: 'reset', ...result });
        load();
      },
    });

  const runConfirmed = async () => {
    const { action } = confirmBox;
    setConfirmBox(null);
    try {
      await action();
    } catch (err) {
      setError(err.message);
    }
  };

  const setField = (name, value) => setForm({ ...form, [name]: value });

  const toggleInList = (name, id) => {
    const list = form[name];
    setField(name, list.includes(id) ? list.filter((x) => x !== id) : [...list, id]);
  };

  const visible = items.filter((it) => JSON.stringify(it).toLowerCase().includes(search.toLowerCase()));

  const renderInput = (f) => {
    if (f.type === 'textarea') {
      return <textarea rows={4} value={form[f.name]} onChange={(e) => setField(f.name, e.target.value)} />;
    }
    if (f.type === 'ref') {
      return (
        <select required={f.required} value={form[f.name]} onChange={(e) => setField(f.name, e.target.value)}>
          <option value="">— select —</option>
          {(options[f.source] ?? []).map((o) => (
            <option key={o.id} value={o.id}>{f.show(o)}</option>
          ))}
        </select>
      );
    }
    if (f.type === 'multiref') {
      return (
        <div className="checklist">
          {(options[f.source] ?? []).length === 0 && <em>None available</em>}
          {(options[f.source] ?? []).map((o) => (
            <label key={o.id}>
              <input type="checkbox" checked={form[f.name].includes(o.id)} onChange={() => toggleInList(f.name, o.id)} />
              {f.show(o)}
            </label>
          ))}
        </div>
      );
    }
    if (isFile(f)) {
      const chosen = form[f.name];
      return (
        <>
          <input
            type="file"
            accept={f.accept}
            required={f.required && !editingId}
            onChange={(e) => setField(f.name, e.target.files[0] ?? null)}
          />
          {chosen && <small className="muted">{chosen.name} ({formatSize(chosen.size)})</small>}
          {editingId && !chosen && <small className="muted">Leave empty to keep the current file.</small>}
        </>
      );
    }
    return (
      <input
        type={f.type ?? 'text'}
        required={f.required}
        value={form[f.name]}
        onChange={(e) => setField(f.name, e.target.value)}
      />
    );
  };

  return (
    <div>
      <div className="page-header">
        <h2>{title}</h2>
        <button className="btn" onClick={() => openForm()}>+ Add {singular}</button>
      </div>

      {error && !form && <p className="error">{error}</p>}

      <input
        className="search"
        placeholder={`Search ${title.toLowerCase()}…`}
        value={search}
        onChange={(e) => setSearch(e.target.value)}
      />

      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>ID</th>
              {tableFields.map((f) => <th key={f.name}>{f.label}</th>)}
              <th></th>
            </tr>
          </thead>
          <tbody>
            {visible.length === 0 && (
              <tr><td colSpan={tableFields.length + 2} className="empty">Nothing here yet.</td></tr>
            )}
            {visible.map((item) => (
              <tr key={item.id}>
                <td>{item.id}</td>
                {tableFields.map((f) => <td key={f.name}>{display(f, item[f.name], item)}</td>)}
                <td className="actions">
                  {entity.rowAction && (
                    <button className="btn btn-small btn-light" onClick={() => setActionItem(item)}>{entity.rowAction.label}</button>
                  )}
                  {entity.resetPassword && (
                    <button className="btn btn-small btn-light" onClick={() => resetPassword(item)}>Reset password</button>
                  )}
                  <button className="btn btn-small" onClick={() => openForm(item)}>Edit</button>
                  <button className="btn btn-small btn-danger" onClick={() => remove(item)}>Delete</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {form && (
        <div className="modal-backdrop" onClick={closeForm}>
          <form className="modal" onSubmit={save} onClick={(e) => e.stopPropagation()}>
            <h3>{editingId ? `Edit ${singular}` : `New ${singular}`}</h3>
            {error && <p className="error">{error}</p>}

            {fields.filter(editable).map((f) => (
              <label key={f.name} className="field">
                <span>{f.label}{f.required && (!isFile(f) || !editingId) && ' *'}</span>
                {renderInput(f)}
              </label>
            ))}

            {uploading && (
              <div className="progress">
                <div className="progress-bar" style={{ width: `${progress}%` }} />
                <span>{progress < 100 ? `Uploading… ${progress}%` : 'Saving…'}</span>
              </div>
            )}

            <div className="modal-actions">
              <button type="button" className="btn btn-light" onClick={closeForm} disabled={uploading}>Cancel</button>
              <button type="submit" className="btn" disabled={uploading}>{uploading ? 'Uploading…' : 'Save'}</button>
            </div>
          </form>
        </div>
      )}

      {actionItem && <RowAction item={actionItem} onClose={closeAction} />}

      {confirmBox && (
        <div className="modal-backdrop" onClick={() => setConfirmBox(null)}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <h3>{confirmBox.title}</h3>
            <p>{confirmBox.message}</p>
            <div className="modal-actions">
              <button className="btn btn-light" onClick={() => setConfirmBox(null)}>Cancel</button>
              <button className={`btn ${confirmBox.danger ? 'btn-danger' : ''}`} onClick={runConfirmed}>
                {confirmBox.button}
              </button>
            </div>
          </div>
        </div>
      )}

      {credentials && (
        <div className="modal-backdrop">
          <div className="modal">
            {credentials.kind === 'reset' ? (
              <>
                <h3>✅ Password reset</h3>
                <p>
                  The password for <strong>{credentials.name}</strong> has been reset.
                  Their old password no longer works. The new one-time password is:
                </p>
              </>
            ) : (
              <>
                <h3>✅ Student added</h3>
                <p>A login was created for <strong>{credentials.name}</strong>:</p>
              </>
            )}
            <div className="credentials">
              <div><span>Username</span><code>{credentials.username}</code></div>
              <div><span>One-time password</span><code>{credentials.password}</code></div>
            </div>
            <p className="muted small">
              Give these to the student. They will be asked to choose their own password the first time
              they log in. <strong>This password is only shown once.</strong>
            </p>
            <div className="modal-actions">
              <button className="btn" onClick={() => setCredentials(null)}>Done</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
