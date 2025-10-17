"use client"

import { useState, useRef } from "react"
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar"
import { Button } from "@/components/ui/button"
import { Camera, Upload, X } from "lucide-react"
import { cn } from "@/lib/utils"

interface ProfileImageUploadProps {
  currentImage?: string | null
  userName: string
  onImageChange: (file: File | null) => void
  isEditing?: boolean
  className?: string
}

export function ProfileImageUpload({
  currentImage,
  userName,
  onImageChange,
  isEditing = false,
  className
}: ProfileImageUploadProps) {
  const [previewImage, setPreviewImage] = useState<string | null>(currentImage || null)
  const [isUploading, setIsUploading] = useState(false)
  const fileInputRef = useRef<HTMLInputElement>(null)

  const handleFileSelect = (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0]
    if (!file) return

    // 파일 유효성 검사
    if (!file.type.startsWith('image/')) {
      alert('이미지 파일만 업로드 가능합니다.')
      return
    }

    if (file.size > 5 * 1024 * 1024) { // 5MB 제한
      alert('파일 크기는 5MB 이하여야 합니다.')
      return
    }

    // 미리보기 이미지 생성
    const reader = new FileReader()
    reader.onload = (e) => {
      setPreviewImage(e.target?.result as string)
    }
    reader.readAsDataURL(file)

    // 부모 컴포넌트에 파일 전달
    onImageChange(file)
  }

  const handleRemoveImage = () => {
    setPreviewImage(null)
    onImageChange(null)
    if (fileInputRef.current) {
      fileInputRef.current.value = ''
    }
  }

  const handleCameraClick = () => {
    fileInputRef.current?.click()
  }

  return (
    <div className={cn("relative", className)}>
      <Avatar className="h-20 w-20">
        <AvatarImage src={previewImage || undefined} />
        <AvatarFallback className="text-lg bg-[#009591] text-white">
          {userName?.charAt(0)?.toUpperCase() || "U"}
        </AvatarFallback>
      </Avatar>

      {isEditing && (
        <>
          <Button
            size="sm"
            variant="outline"
            className="absolute -bottom-2 -right-2 h-8 w-8 rounded-full p-0 bg-white border-2 border-white shadow-sm hover:bg-gray-50"
            onClick={handleCameraClick}
            disabled={isUploading}
          >
            {isUploading ? (
              <div className="h-4 w-4 animate-spin rounded-full border-2 border-[#009591] border-t-transparent" />
            ) : (
              <Camera className="h-4 w-4" />
            )}
          </Button>

          {previewImage && previewImage !== currentImage && (
            <Button
              size="sm"
              variant="outline"
              className="absolute -top-2 -right-2 h-6 w-6 rounded-full p-0 bg-red-100 border-red-200 text-red-600 hover:bg-red-200"
              onClick={handleRemoveImage}
            >
              <X className="h-3 w-3" />
            </Button>
          )}

          <input
            ref={fileInputRef}
            type="file"
            accept="image/*"
            onChange={handleFileSelect}
            className="hidden"
            aria-label="프로필 이미지 업로드"
          />
        </>
      )}
    </div>
  )
}
