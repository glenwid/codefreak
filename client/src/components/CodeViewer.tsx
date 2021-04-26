import { FileUnknownOutlined } from '@ant-design/icons'
import { Card, Result } from 'antd'
import { basename, extname } from 'path'
import React from 'react'
import { FileType, useGetAnswerFileQuery } from '../generated/graphql'
import { isBinaryContent, numberOfLines, sliceLines } from '../services/file'
import AsyncPlaceholder from './AsyncContainer'
import Centered from './Centered'
import './CodeViewer.less'
import { Language } from 'prism-react-renderer'
import { CodeReview } from 'react-code-review-editor'

interface CodeViewerProps {
  answerId: string
  path: string
  lineStart?: number
  lineEnd?: number
  numContextRows?: number
  review?: boolean
}

const codeViewerMessage = (message: React.ReactNode) => {
  return (
    <Centered>
      <Result title={message} icon={<FileUnknownOutlined />} />
    </Centered>
  )
}

const CodeViewer: React.FC<CodeViewerProps> = ({
  answerId,
  path: queryPath,
  lineStart,
  lineEnd,
  numContextRows = 3,
  review
}) => {
  const result = useGetAnswerFileQuery({
    variables: { id: answerId, path: queryPath }
  })

  if (result.data === undefined) {
    return <AsyncPlaceholder result={result} />
  }

  // use path from response or content and path can by out-of-sync
  const { content, type, path } = result.data.answerFile

  if (type !== FileType.File) {
    return codeViewerMessage(
      <>
        <code>${basename(path)}</code> is a {type.toLowerCase()}
      </>
    )
  }

  let value = content || ''

  if (isBinaryContent(value)) {
    return codeViewerMessage(
      <>
        <code>{basename(path)}</code> is a binary file
      </>
    )
  }

  const highlightLines = []
  let firstLineNumber = lineStart || 1
  if (lineStart) {
    highlightLines.push(lineStart)
    firstLineNumber = Math.max(lineStart - numContextRows, 1)
    const end = Math.min(
      (lineEnd || lineStart) + numContextRows + 1,
      numberOfLines(value)
    )
    value = sliceLines(value, firstLineNumber, end)
  }

  const getLanguage = (): Language => {
    let language: Language
    switch (extname(path)) {
      case 'js':
        language = 'javascript'
        return language
      case 'c':
        language = 'c'
        return language
      case 'py':
        language = 'python'
        return language
      default:
        language = 'javascript'
        return language
    }
  }

  if (review !== true) {
    return (
      <CodeReview
        code={value}
        language={getLanguage()}
        onCommentCreated={(value) => console.log(value)}
      />
    )
  }

  return (
    <CodeReview
      code={value}
      language={getLanguage()}
      onCommentCreated={(value) => console.log(value)}
    />
  )
}

export const CodeViewerCard: React.FC<CodeViewerProps> = props => {
  const { path } = props
  return (
    <Card title={path} size="small" className="code-viewer-card">
      <CodeViewer {...props} />
    </Card>
  )
}

export default CodeViewer
