from app.db.json_repository import JsonAnalysisStore
from app.db.repository import to_analysis_dto
from app.models.enums import AnalysisStatus
from app.models.schemas import LLMAnalysisResult


async def test_json_repository_persists_analysis(tmp_path):
    store = JsonAnalysisStore(str(tmp_path / "ai_analysis.json"))

    async with store.repository() as repository:
        pending = await repository.create_pending("trace-123", "alert-123")
        await repository.mark_processing(pending.analysis_id)
        completed = await repository.mark_completed(
            pending.analysis_id,
            LLMAnalysisResult(
                root_cause="Database timeout",
                affected_services=["orders"],
                recommendations=["Check database capacity"],
                confidence_score=0.9,
            ),
            model_used="stub",
            prompt_tokens=10,
            completion_tokens=5,
        )

    async with store.repository() as repository:
        reloaded = await repository.get_by_trace_id("trace-123")

    assert reloaded is not None
    assert reloaded.analysis_id == completed.analysis_id
    assert reloaded.status == AnalysisStatus.COMPLETED
    assert to_analysis_dto(reloaded).root_cause == "Database timeout"
