import { useEffect, useMemo, useState } from 'react'
import { useQuery, useMutation } from '@tanstack/react-query'
import { fetchMetadata, searchGraph } from '@/api/client'
import type { JsonValue, MetadataInputDef, SearchRequest } from '@/types/api'
import { useGraphStore } from '@/store/graphStore'

function InputField({ def, value, onChange }: { def: MetadataInputDef; value: JsonValue; onChange: (v: JsonValue) => void }) {
	return (
		<div className="form-row">
			<label className="label">{def.label ?? def.name}{def.required ? ' *' : ''}</label>
			{def.type === 'enum' ? (
				<select className="select" value={String(value ?? '')} onChange={(e) => onChange(e.target.value)}>
					<option value="">Select...</option>
					{(def.options ?? []).map((opt) => (
						<option key={opt} value={opt}>{opt}</option>
					))}
				</select>
			) : (
				<input
					className="input"
					type={def.type === 'number' ? 'number' : def.type === 'date' ? 'date' : 'text'}
					value={String(value ?? '')}
					onChange={(e) => onChange(def.type === 'number' ? Number(e.target.value) : e.target.value)}
				/>
			)}
		</div>
	)
}

export default function DynamicForm() {
	const { data: metadata, isLoading } = useQuery({ queryKey: ['metadata'], queryFn: fetchMetadata, staleTime: 1000 * 60 * 60 })
	const [refType, setRefType] = useState<string>('')
	const [queryBy, setQueryBy] = useState<string>('')
	const [inputs, setInputs] = useState<Record<string, JsonValue>>({})
	const mergeGraph = useGraphStore((s) => s.mergeGraph)
	const resetGraph = useGraphStore((s) => s.resetGraph)

	useEffect(() => {
		if (metadata && !refType) {
			const first = Object.keys(metadata.referenceDataTypes)[0]
			setRefType(first ?? '')
		}
	}, [metadata])

	useEffect(() => {
		if (!refType || !metadata) return
		const qb = Object.keys(metadata.referenceDataTypes[refType]?.queryBy ?? {})
		setQueryBy(qb[0] ?? '')
	}, [refType, metadata])

	const inputDefs = useMemo(() => {
		if (!metadata || !refType || !queryBy) return [] as MetadataInputDef[]
		return metadata.referenceDataTypes[refType]?.queryBy?.[queryBy]?.inputs ?? []
	}, [metadata, refType, queryBy])

	useEffect(() => {
		const defaults: Record<string, JsonValue> = {}
		for (const d of inputDefs) defaults[d.name] = ''
		setInputs(defaults)
	}, [inputDefs])

	const { mutate: doSearch, isPending } = useMutation({
		mutationFn: async () => {
			const req: SearchRequest = { referenceDataType: refType, queryBy, inputs, graphOptions: { maxDepth: 1, maxNeighborsPerType: 50, includeFacets: true } }
			return searchGraph(req)
		},
		onSuccess: (data) => {
			resetGraph()
			mergeGraph(data.graph.nodes, data.graph.edges)
		}
	})

	if (isLoading) return <div>Loading metadata…</div>
	if (!metadata) return <div>Metadata not available</div>

	const refTypes = Object.keys(metadata.referenceDataTypes)
	const queryByOptions = Object.keys(metadata.referenceDataTypes[refType]?.queryBy ?? {})

	return (
		<div>
			<div className="section-title">Search</div>
			<div className="form-row">
				<label className="label">Reference Data Type</label>
				<select className="select" value={refType} onChange={(e) => setRefType(e.target.value)}>
					{refTypes.map((t) => (
						<option key={t} value={t}>{t}</option>
					))}
				</select>
			</div>
			<div className="form-row">
				<label className="label">Query By</label>
				<select className="select" value={queryBy} onChange={(e) => setQueryBy(e.target.value)}>
					{queryByOptions.map((q) => (
						<option key={q} value={q}>{q}</option>
					))}
				</select>
			</div>
			{inputDefs.map((def) => (
				<InputField key={def.name} def={def} value={inputs[def.name] ?? ''} onChange={(v) => setInputs((prev) => ({ ...prev, [def.name]: v }))} />
			))}
			<button className="button" onClick={() => doSearch()} disabled={isPending || !refType || !queryBy}>
				{isPending ? 'Searching…' : 'Search'}
			</button>
		</div>
	)
}