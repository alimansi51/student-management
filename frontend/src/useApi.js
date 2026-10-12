import { useCallback, useEffect, useState } from 'react';
import { api } from './api';

/** Loads `path` from the API when the component mounts. `data` is null while loading. */
export function useApi(path) {
  const [data, setData] = useState(null);
  const [error, setError] = useState('');

  const reload = useCallback(() => {
    api(path).then(setData).catch((e) => setError(e.message));
  }, [path]);

  useEffect(() => {
    reload();
  }, [reload]);

  return { data, error, reload };
}
