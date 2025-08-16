import { useEffect } from 'react'
import { useQuery } from '@tanstack/react-query'
import { fetchNodePayload } from '@/api/client'
import { useGraphStore } from '@/store/graphStore'

export default function PayloadPanel() {
	const selectedNodeId = useGraphStore((s) => s.selectedNodeId)
	const setPayload = useGraphStore((s) => s.setPayload)
	const payload = useGraphStore((s) => s.selectedPayload)

	const { data } = useQuery({
		queryKey: ['payload', selectedNodeId],
		queryFn: () => fetchNodePayload(selectedNodeId as string),
		enabled: Boolean(selectedNodeId),
		staleTime: 1000 * 60 * 60
	})

	useEffect(() => { if (data) setPayload(data) }, [data])

	return (
		<div>
			<div className="section-title">Payload</div>
			{!selectedNodeId ? (
				<div className="payload">Select a node to view its payload.</div>
			) : payload ? (
				<pre className="payload">{JSON.stringify(payload, null, 2)}</pre>
			) : (
				<div className="payload">Loading…</div>
			)}
		</div>
	)
}