import axios from 'axios'
import type { ExpandRequest, ExpandResponse, GraphResponse, MetadataResponse, NodePayload, SearchRequest } from '@/types/api'

const API_BASE = (import.meta as any).env?.VITE_API_BASE_URL || (import.meta as any).env?.VITE_API_BASE || 'http://localhost:8080'

export const http = axios.create({
	baseURL: API_BASE,
	headers: { 'Content-Type': 'application/json' }
})

export async function fetchMetadata(): Promise<MetadataResponse> {
	const { data } = await http.get<MetadataResponse>('/metadata/types')
	return data
}

export async function searchGraph(req: SearchRequest): Promise<GraphResponse> {
	const { data } = await http.post<GraphResponse>('/graph/search', req)
	return data
}

export async function expandGraph(req: ExpandRequest): Promise<ExpandResponse> {
	const { data } = await http.post<ExpandResponse>('/graph/expand', req)
	return data
}

export async function fetchNodePayload(id: string): Promise<NodePayload> {
	const { data } = await http.get<NodePayload>(`/node/${encodeURIComponent(id)}`)
	return data
}