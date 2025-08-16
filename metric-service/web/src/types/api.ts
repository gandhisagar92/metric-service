export type JsonPrimitive = string | number | boolean | null
export type JsonMap = { [k: string]: JsonValue }
export type JsonArray = JsonValue[]
export type JsonValue = JsonPrimitive | JsonMap | JsonArray

export interface MetadataInputDef {
	name: string
	label?: string
	type: 'string' | 'number' | 'date' | 'enum'
	required?: boolean
	options?: string[]
}

export interface MetadataResponse {
	version: string
	referenceDataTypes: Record<string, {
		queryBy: Record<string, { inputs: MetadataInputDef[] }>
	}>
}

export interface GraphNode {
	id: string
	type: string
	label?: string
	attributes?: Record<string, JsonValue>
	meta?: Record<string, JsonValue>
}

export interface GraphEdge {
	id: string
	type: string
	label?: string
	source: string
	target: string
	attributes?: Record<string, JsonValue>
}

export interface AdjacencyCounts { [relation: string]: number }
export interface PageInfo { total?: number; pageSize?: number; returned?: number; cursor?: string }

export interface SearchRequest {
	referenceDataType: string
	queryBy: string
	inputs: Record<string, JsonValue>
	graphOptions?: {
		maxDepth?: number
		maxNeighborsPerType?: number
		includeFacets?: boolean
	}
}

export interface GraphResponse {
	graph: {
		nodes: GraphNode[]
		edges: GraphEdge[]
		adjacency?: Record<string, {
			counts?: Record<string, number>
			pageInfo?: Record<string, PageInfo>
		}>
	}
	facets?: Record<string, Record<string, { value: string; count: number }[]>>
}

export type FilterOp = 'EQ' | 'IN' | 'NEQ' | 'GT' | 'GTE' | 'LT' | 'LTE' | 'CONTAINS' | 'PREFIX'

export interface ExpandRequest {
	nodeId: string
	relations?: string[]
	filters?: Array<{ scope: 'node' | 'edge'; field: string; op: FilterOp; value: JsonValue }>
	pagination?: { relation?: string; cursor?: string; limit?: number }
	sort?: { field: string; direction: 'asc' | 'desc' }
}

export interface ExpandResponse {
	nodes: GraphNode[]
	edges: GraphEdge[]
	facets?: GraphResponse['facets']
	pageInfo?: Record<string, PageInfo>
}

export interface NodePayload {
	id: string
	type: string
	displayLabel?: string
	attributes: Record<string, JsonValue>
	audit?: Record<string, JsonValue>
}