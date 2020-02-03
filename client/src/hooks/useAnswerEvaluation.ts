import { useEffect, useState } from 'react'
import {
  LatestEvaluationFragment,
  PendingEvaluationStatus,
  useGetLatestEvaluationStatusLazyQuery
} from '../generated/graphql'
import usePendingEvaluationUpdated from './usePendingEvaluationUpdated'

const useAnswerEvaluation = (
  answerId: string,
  initialLatestEvaluation: LatestEvaluationFragment | null,
  initialPendingEvaluationStatus: PendingEvaluationStatus | null
) => {
  const [
    pendingEvaluationStatus,
    setPendingEvaluationStatus
  ] = useState<PendingEvaluationStatus | null>(initialPendingEvaluationStatus)

  usePendingEvaluationUpdated(answerId, newStatus => {
    setPendingEvaluationStatus(newStatus)
    // start fetching new evaluation results if status changed to finished
    if (newStatus === PendingEvaluationStatus.Finished) {
      fetchLatestEvaluation()
    }
  })

  const [
    latestEvaluation,
    setLatestEvaluation
  ] = useState<LatestEvaluationFragment | null>(initialLatestEvaluation)
  const [
    fetchLatestEvaluation,
    latestEvaluationQuery
  ] = useGetLatestEvaluationStatusLazyQuery({
    variables: { answerId },
    fetchPolicy: 'network-only'
  })

  useEffect(() => {
    if (
      latestEvaluationQuery.data &&
      latestEvaluationQuery.data.answer.latestEvaluation
    ) {
      setLatestEvaluation(latestEvaluationQuery.data.answer.latestEvaluation)
    }
  }, [setLatestEvaluation, latestEvaluationQuery, latestEvaluationQuery.data])

  return {
    latestEvaluation,
    pendingEvaluationStatus,
    loading: latestEvaluationQuery.loading
  }
}

export default useAnswerEvaluation
