'use client'

import { useEditor, EditorContent } from '@tiptap/react'
import StarterKit from '@tiptap/starter-kit'
import Image from '@tiptap/extension-image'
import Link from '@tiptap/extension-link'
import TextAlign from '@tiptap/extension-text-align'
import Underline from '@tiptap/extension-underline'
import Color from '@tiptap/extension-color'
import TextStyle from '@tiptap/extension-text-style'
import { useCallback, useEffect } from 'react'
import { cn } from '@/lib/utils'
import { Button } from '@/components/ui/button'
import { uploadCampaignImage } from '@/lib/api/campaign-api'
import { toast } from 'sonner'
import { HtmlContent } from '@/components/ui/html-content'

// HTML을 백엔드 호환 형식으로 정리하는 함수
function cleanupHtmlForBackend(html: string): string {
  if (!html) return ''
  
  // DOM parser를 사용하여 HTML을 정리
  const parser = new DOMParser()
  const doc = parser.parseFromString(html, 'text/html')
  
  // 모든 요소에서 class, style 등의 속성 제거
  const allElements = doc.querySelectorAll('*')
  allElements.forEach(element => {
    // 기본 HTML 태그 속성만 유지하고 나머지 제거
    const allowedAttributes = ['href', 'src', 'alt', 'title']
    const attributesToRemove = []
    
    for (let i = 0; i < element.attributes.length; i++) {
      const attr = element.attributes[i]
      if (!allowedAttributes.includes(attr.name)) {
        attributesToRemove.push(attr.name)
      }
    }
    
    attributesToRemove.forEach(attrName => {
      element.removeAttribute(attrName)
    })
  })
  
  // body 내용만 반환 (html, head 태그 제거)
  return doc.body.innerHTML
}

import {
  Bold,
  Italic,
  Underline as UnderlineIcon,
  Strikethrough,
  Heading1,
  Heading2,
  Heading3,
  List,
  ListOrdered,
  Quote,
  Code,
  AlignLeft,
  AlignCenter,
  AlignRight,
  Link as LinkIcon,
  Image as ImageIcon,
  Undo,
  Redo
} from 'lucide-react'

interface RichTextEditorProps {
  value: string
  onChange: (value: string) => void
  placeholder?: string
  className?: string
  readOnly?: boolean
  minHeight?: string
}

export function RichTextEditor({
  value,
  onChange,
  placeholder = '내용을 입력하세요...',
  className,
  readOnly = false,
  minHeight = '200px'
}: RichTextEditorProps) {
  const editor = useEditor({
    extensions: [
      StarterKit.configure({
        paragraph: {
          HTMLAttributes: {},
        },
      }),
      Underline,
      TextStyle,
      Color,
      TextAlign.configure({
        types: ['heading', 'paragraph'],
      }),
      Link.configure({
        openOnClick: false,
        HTMLAttributes: {},
      }),
      Image.configure({
        HTMLAttributes: {},
      })
    ],
    content: value || '',
    editable: !readOnly,
    immediatelyRender: false,
    editorProps: {
      attributes: {
        class: 'prose prose-sm max-w-none focus:outline-none min-h-[200px] p-3',
        'data-placeholder': placeholder,
      },
    },
    onUpdate: ({ editor }) => {
      const html = editor.getHTML()
      const cleanHtml = cleanupHtmlForBackend(html)
      onChange(cleanHtml)
    },
    onCreate: ({ editor }) => {
      // 에디터가 생성되면 포커스 가능하도록 설정
      editor.setEditable(!readOnly)
    }
  })

  useEffect(() => {
    if (editor && editor.getHTML() !== value) {
      editor.commands.setContent(value)
    }
  }, [value, editor])

  const addImage = useCallback(async () => {
    const input = document.createElement('input')
    input.type = 'file'
    input.accept = 'image/*'
    input.onchange = async () => {
      const file = input.files?.[0]
      if (file) {
        if (!file.type.startsWith('image/')) {
          toast.error('이미지 파일만 업로드 가능합니다.')
          return
        }

        if (file.size > 5 * 1024 * 1024) { // 5MB 제한 (Base64는 용량이 커짐)
          toast.error('파일 크기는 5MB 이하여야 합니다.')
          return
        }

        try {
          toast.loading('이미지를 업로드하는 중입니다...', { id: 'image-upload' })
          
          // 백엔드 서버로 이미지 업로드
          const response = await uploadCampaignImage(file)
          if (editor) {
            editor.chain().focus().setImage({ 
              src: response.imageUrl, 
              alt: response.originalFileName 
            }).run()
          }
          toast.success('이미지가 성공적으로 추가되었습니다!', { id: 'image-upload' })

        } catch (error) {
          console.error('이미지 업로드 실패:', error)
          toast.error('이미지 업로드에 실패했습니다. 다시 시도해 주세요.', { id: 'image-upload' })
        }
      }
    }
    input.click()
  }, [editor])

  const addLink = useCallback(() => {
    const previousUrl = editor?.getAttributes('link').href
    const url = window.prompt('URL', previousUrl)

    if (url === null) {
      return
    }

    if (url === '') {
      editor?.chain().focus().extendMarkRange('link').unsetLink().run()
      return
    }

    editor?.chain().focus().extendMarkRange('link').setLink({ href: url }).run()
  }, [editor])

  if (!editor) {
    return <div className={cn('w-full border rounded-md', className)} style={{ minHeight }} />
  }

  if (readOnly) {
    return (
      <div className={cn('prose prose-sm max-w-none', className)}>
        <EditorContent editor={editor} />
      </div>
    )
  }

  return (
    <div className={cn('border border-input rounded-md', className)}>
      {/* 툴바 */}
      <div className="border-b p-2 flex flex-wrap gap-1 bg-muted/50">
        {/* 실행취소/다시실행 */}
        <Button
          variant="ghost"
          size="sm"
          onClick={() => editor.chain().focus().undo().run()}
          disabled={!editor.can().undo()}
          className="h-8 px-2"
        >
          <Undo className="h-4 w-4" />
        </Button>
        <Button
          variant="ghost"
          size="sm"
          onClick={() => editor.chain().focus().redo().run()}
          disabled={!editor.can().redo()}
          className="h-8 px-2"
        >
          <Redo className="h-4 w-4" />
        </Button>

        <div className="w-px h-8 bg-border" />

        {/* 제목 */}
        <Button
          variant={editor.isActive('heading', { level: 1 }) ? 'default' : 'ghost'}
          size="sm"
          onClick={() => editor.chain().focus().toggleHeading({ level: 1 }).run()}
          className="h-8 px-2"
        >
          <Heading1 className="h-4 w-4" />
        </Button>
        <Button
          variant={editor.isActive('heading', { level: 2 }) ? 'default' : 'ghost'}
          size="sm"
          onClick={() => editor.chain().focus().toggleHeading({ level: 2 }).run()}
          className="h-8 px-2"
        >
          <Heading2 className="h-4 w-4" />
        </Button>
        <Button
          variant={editor.isActive('heading', { level: 3 }) ? 'default' : 'ghost'}
          size="sm"
          onClick={() => editor.chain().focus().toggleHeading({ level: 3 }).run()}
          className="h-8 px-2"
        >
          <Heading3 className="h-4 w-4" />
        </Button>

        <div className="w-px h-8 bg-border" />

        {/* 텍스트 서식 */}
        <Button
          variant={editor.isActive('bold') ? 'default' : 'ghost'}
          size="sm"
          onClick={() => editor.chain().focus().toggleBold().run()}
          className="h-8 px-2"
        >
          <Bold className="h-4 w-4" />
        </Button>
        <Button
          variant={editor.isActive('italic') ? 'default' : 'ghost'}
          size="sm"
          onClick={() => editor.chain().focus().toggleItalic().run()}
          className="h-8 px-2"
        >
          <Italic className="h-4 w-4" />
        </Button>
        <Button
          variant={editor.isActive('underline') ? 'default' : 'ghost'}
          size="sm"
          onClick={() => editor.chain().focus().toggleUnderline().run()}
          className="h-8 px-2"
        >
          <UnderlineIcon className="h-4 w-4" />
        </Button>
        <Button
          variant={editor.isActive('strike') ? 'default' : 'ghost'}
          size="sm"
          onClick={() => editor.chain().focus().toggleStrike().run()}
          className="h-8 px-2"
        >
          <Strikethrough className="h-4 w-4" />
        </Button>

        <div className="w-px h-8 bg-border" />

        {/* 정렬 */}
        <Button
          variant={editor.isActive({ textAlign: 'left' }) ? 'default' : 'ghost'}
          size="sm"
          onClick={() => editor.chain().focus().setTextAlign('left').run()}
          className="h-8 px-2"
        >
          <AlignLeft className="h-4 w-4" />
        </Button>
        <Button
          variant={editor.isActive({ textAlign: 'center' }) ? 'default' : 'ghost'}
          size="sm"
          onClick={() => editor.chain().focus().setTextAlign('center').run()}
          className="h-8 px-2"
        >
          <AlignCenter className="h-4 w-4" />
        </Button>
        <Button
          variant={editor.isActive({ textAlign: 'right' }) ? 'default' : 'ghost'}
          size="sm"
          onClick={() => editor.chain().focus().setTextAlign('right').run()}
          className="h-8 px-2"
        >
          <AlignRight className="h-4 w-4" />
        </Button>

        <div className="w-px h-8 bg-border" />

        {/* 리스트 */}
        <Button
          variant={editor.isActive('bulletList') ? 'default' : 'ghost'}
          size="sm"
          onClick={() => editor.chain().focus().toggleBulletList().run()}
          className="h-8 px-2"
        >
          <List className="h-4 w-4" />
        </Button>
        <Button
          variant={editor.isActive('orderedList') ? 'default' : 'ghost'}
          size="sm"
          onClick={() => editor.chain().focus().toggleOrderedList().run()}
          className="h-8 px-2"
        >
          <ListOrdered className="h-4 w-4" />
        </Button>

        <div className="w-px h-8 bg-border" />

        {/* 인용 및 코드 */}
        <Button
          variant={editor.isActive('blockquote') ? 'default' : 'ghost'}
          size="sm"
          onClick={() => editor.chain().focus().toggleBlockquote().run()}
          className="h-8 px-2"
        >
          <Quote className="h-4 w-4" />
        </Button>
        <Button
          variant={editor.isActive('code') ? 'default' : 'ghost'}
          size="sm"
          onClick={() => editor.chain().focus().toggleCode().run()}
          className="h-8 px-2"
        >
          <Code className="h-4 w-4" />
        </Button>

        <div className="w-px h-8 bg-border" />

        {/* 링크 및 이미지 */}
        <Button
          variant={editor.isActive('link') ? 'default' : 'ghost'}
          size="sm"
          onClick={addLink}
          className="h-8 px-2"
        >
          <LinkIcon className="h-4 w-4" />
        </Button>
        <Button
          variant="ghost"
          size="sm"
          onClick={addImage}
          className="h-8 px-2"
        >
          <ImageIcon className="h-4 w-4" />
        </Button>
      </div>

      {/* 에디터 */}
      <div style={{ minHeight }}>
        <EditorContent 
          editor={editor} 
          className="focus:outline-none"
        />
      </div>

      {/* 커스텀 스타일 */}
      <style jsx global>{`
        .ProseMirror {
          outline: none !important;
          padding: 12px;
          border: none;
          min-height: ${minHeight};
          cursor: text;
        }
        
        .ProseMirror p.is-editor-empty:first-child::before {
          color: #9ca3af;
          content: attr(data-placeholder);
          float: left;
          height: 0;
          pointer-events: none;
          font-style: italic;
        }
        
        .ProseMirror:focus {
          outline: none !important;
        }
        
        .ProseMirror img {
          max-width: 100%;
          height: auto;
          border-radius: 8px;
        }
        
        .ProseMirror blockquote {
          border-left: 4px solid #009591;
          margin: 16px 0;
          padding-left: 16px;
          font-style: italic;
        }
        
        .ProseMirror code {
          background-color: #f1f5f9;
          padding: 2px 4px;
          border-radius: 4px;
          font-family: monospace;
        }
        
        .dark .ProseMirror {
          color: #f9fafb;
        }
        
        .dark .ProseMirror code {
          background-color: #1e293b;
        }
        
        .dark .ProseMirror p.is-editor-empty:first-child::before {
          color: #6b7280;
        }
        
        .ProseMirror p {
          margin: 0.5em 0;
        }
        
        .ProseMirror p:first-child {
          margin-top: 0;
        }
        
        .ProseMirror p:last-child {
          margin-bottom: 0;
        }
      `}</style>
    </div>
  )
}

// HTML 콘텐츠를 표시하는 읽기 전용 컴포넌트
interface RichTextViewerProps {
  html: string
  className?: string
}

export function RichTextViewer({ html, className }: RichTextViewerProps) {
  return (
    <HtmlContent 
      html={html}
      className={cn('rich-text-viewer', className)}
    />
  )
}