import { useEffect, useMemo, useRef } from 'react'
import Graph from 'graphology'
import Sigma from 'sigma'
import { useGraphStore } from '@/store/graphStore'

function useSigmaGraph() {
	const containerRef = useRef<HTMLDivElement | null>(null)
	const sigmaRef = useRef<Sigma | null>(null)
	const graphRef = useRef(new Graph())

	useEffect(() => {
		if (!containerRef.current) return
		const renderer = new Sigma(graphRef.current, containerRef.current, {})
		sigmaRef.current = renderer
		return () => { renderer.kill(); sigmaRef.current = null }
	}, [])

	return { containerRef, sigmaRef, graphRef }
}

export default function GraphView() {
	const { containerRef, sigmaRef, graphRef } = useSigmaGraph()
	const nodes = useGraphStore((s) => s.nodes)
	const edges = useGraphStore((s) => s.edges)
	const setSelected = useGraphStore((s) => s.setSelected)

	const nodeArray = useMemo(() => Object.values(nodes), [nodes])
	const edgeArray = useMemo(() => Object.values(edges), [edges])

	useEffect(() => {
		const g = graphRef.current
		g.clear()
		for (const n of nodeArray) {
			g.addNode(n.id, {
				label: n.label ?? n.id,
				size: Math.max(4, Math.min(18, Number(n.meta?.degree ?? 1) ** 0.25)),
				color: '#60a5fa'
			})
		}
		for (const e of edgeArray) {
			if (!g.hasNode(e.source) || !g.hasNode(e.target)) continue
			g.addEdgeWithKey(e.id, e.source, e.target, {
				label: e.type,
				size: 1,
				color: '#94a3b8'
			})
		}
		sigmaRef.current?.getCamera().animatedReset({ duration: 300 })
	}, [nodeArray, edgeArray])

	useEffect(() => {
		const s = sigmaRef.current
		if (!s) return
		const handler = (e: any) => { setSelected(e.node) }
		s.on('clickNode', handler)
		return () => { s.off('clickNode', handler) }
	}, [sigmaRef.current])

	return (
		<div className="graph-container" ref={containerRef} />
	)
}