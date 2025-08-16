import DynamicForm from '@/components/DynamicForm'
import PayloadPanel from '@/components/PayloadPanel'
import GraphView from '@/components/GraphView'

export default function App() {
	return (
		<div className="app">
			<div className="left-top">
				<DynamicForm />
			</div>
			<div className="left-bottom">
				<PayloadPanel />
			</div>
			<div className="right">
				<div className="toolbar">
					<span className="badge">Financial Data Relationship Explorer</span>
				</div>
				<GraphView />
			</div>
		</div>
	)
}