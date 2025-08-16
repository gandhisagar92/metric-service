import axios from 'axios';
const API_BASE = import.meta.env?.VITE_API_BASE_URL || import.meta.env?.VITE_API_BASE || 'http://localhost:8080';
export const http = axios.create({
    baseURL: API_BASE,
    headers: { 'Content-Type': 'application/json' }
});
export async function fetchMetadata() {
    const { data } = await http.get('/metadata/types');
    return data;
}
export async function searchGraph(req) {
    const { data } = await http.post('/graph/search', req);
    return data;
}
export async function expandGraph(req) {
    const { data } = await http.post('/graph/expand', req);
    return data;
}
export async function fetchNodePayload(id) {
    const { data } = await http.get(`/node/${encodeURIComponent(id)}`);
    return data;
}
