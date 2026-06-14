from app.services.analysis_service import AnalysisService

analysis_service: AnalysisService | None = None


def get_analysis_service() -> AnalysisService:
    if analysis_service is None:
        raise RuntimeError("Analysis service is not initialized")
    return analysis_service
