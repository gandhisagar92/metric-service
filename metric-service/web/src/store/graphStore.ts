import { create } from 'zustand'
import type { GraphEdge, GraphNode, NodePayload } from '@/types/api'

interface GraphState {
	selectedNodeId: string | null
	selectedPayload: NodePayload | null
	nodes: Record<string, GraphNode>
	edges: Record<string, GraphEdge>
	setSelected: (id: string | null) => void
	setPayload: (payload: NodePayload | null) => void
	mergeGraph: (nodes: GraphNode[], edges: GraphEdge[]) => void
	resetGraph: () => void
}

export const useGraphStore = create<GraphState>((set, get) => ({
	selectedNodeId: null,
	selectedPayload: null,
	nodes: {},
	edges: {},
	setSelected: (id) => set({ selectedNodeId: id }),
	setPayload: (payload) => set({ selectedPayload: payload }),
	mergeGraph: (nodes, edges) => set((state) => {
		const nextNodes = { ...state.nodes }
		for (const n of nodes) nextNodes[n.id] = n
		const nextEdges = { ...state.edges }
		for (const e of edges) nextEdges[e.id] = e
		return { nodes: nextNodes, edges: nextEdges }
	}),
	resetGraph: () => set({ nodes: {}, edges: {}, selectedNodeId: null, selectedPayload: null })
}))