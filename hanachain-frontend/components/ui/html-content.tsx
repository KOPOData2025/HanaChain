"use client"

import DOMPurify from 'isomorphic-dompurify'
import { cn } from "@/lib/utils"

interface HtmlContentProps {
  html: string
  className?: string
  allowedTags?: string[]
  allowedAttributes?: Record<string, string[]>
}

const DEFAULT_ALLOWED_TAGS = [
  'article', 'section', 'div', 'p', 'h1', 'h2', 'h3', 'h4', 'h5', 'h6',
  'strong', 'em', 'u', 'b', 'i', 'span', 'br', 'hr',
  'ul', 'ol', 'li', 'blockquote', 'pre', 'code',
  'img', 'a', 'table', 'thead', 'tbody', 'tr', 'th', 'td',
  'figure', 'figcaption', 'mark', 'del', 'ins', 'sub', 'sup'
]

const DEFAULT_ALLOWED_ATTRIBUTES = {
  '*': ['class', 'id', 'data-*'],
  'img': ['src', 'alt', 'width', 'height', 'loading'],
  'a': ['href', 'target', 'rel', 'title'],
  'table': ['cellpadding', 'cellspacing', 'border'],
  'th': ['scope', 'colspan', 'rowspan'],
  'td': ['colspan', 'rowspan']
}

export function HtmlContent({ 
  html, 
  className, 
  allowedTags = DEFAULT_ALLOWED_TAGS,
  allowedAttributes = DEFAULT_ALLOWED_ATTRIBUTES 
}: HtmlContentProps) {
  // HTML 정화 및 보안 검증
  const sanitizedHtml = DOMPurify.sanitize(html, {
    ALLOWED_TAGS: allowedTags,
    ALLOWED_ATTR: Object.values(allowedAttributes).flat(),
    ALLOW_DATA_ATTR: true,
    FORBID_SCRIPTS: true,
    FORBID_TAGS: ['script', 'object', 'embed', 'form', 'input', 'iframe'],
    FORBID_ATTR: ['onerror', 'onload', 'onclick', 'onmouseover', 'style']
  })

  return (
    <div 
      className={cn(
        "prose prose-gray max-w-none",
        "prose-headings:text-gray-900 prose-headings:font-semibold",
        "prose-p:text-gray-700 prose-p:leading-relaxed",
        "prose-strong:text-gray-900 prose-strong:font-semibold",
        "prose-em:text-gray-800 prose-em:italic",
        "prose-a:text-[#009591] prose-a:no-underline hover:prose-a:underline",
        "prose-img:rounded-lg prose-img:shadow-sm",
        "prose-blockquote:border-l-4 prose-blockquote:border-[#009591]",
        "prose-blockquote:pl-4 prose-blockquote:italic",
        "prose-code:bg-gray-100 prose-code:px-1 prose-code:py-0.5 prose-code:rounded",
        "prose-pre:bg-gray-900 prose-pre:text-white",
        "prose-ul:list-disc prose-ol:list-decimal",
        "prose-li:text-gray-700",
        "prose-table:border-collapse prose-table:w-full",
        "prose-th:border prose-th:border-gray-300 prose-th:bg-gray-50 prose-th:p-2",
        "prose-td:border prose-td:border-gray-300 prose-td:p-2",
        "dark:prose-invert",
        "dark:prose-headings:text-white",
        "dark:prose-p:text-gray-300",
        "dark:prose-strong:text-white",
        "dark:prose-a:text-[#00b5b0]",
        "dark:prose-blockquote:border-[#00b5b0]",
        "dark:prose-code:bg-gray-800",
        "dark:prose-th:bg-gray-800 dark:prose-th:border-gray-700",
        "dark:prose-td:border-gray-700",
        className
      )}
      dangerouslySetInnerHTML={{ __html: sanitizedHtml }}
    />
  )
}