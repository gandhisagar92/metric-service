import { create } from 'zustand';
export const useGraphStore = create((set, get) => ({
    selectedNodeId: null,
    selectedPayload: null,
    nodes: {},
    edges: {},
    setSelected: (id) => set({ selectedNodeId: id }),
    setPayload: (payload) => set({ selectedPayload: payload }),
    mergeGraph: (nodes, edges) => set((state) => {
        const nextNodes = { ...state.nodes };
        for (const n of nodes)
            nextNodes[n.id] = n;
        const nextEdges = { ...state.edges };
        for (const e of edges)
            nextEdges[e.id] = e;
        return { nodes: nextNodes, edges: nextEdges };
    }),
    resetGraph: () => set({ nodes: {}, edges: {}, selectedNodeId: null, selectedPayload: null })
}));
