import { Skeleton } from '@/components/ui/skeleton'
import { Card, CardContent, CardHeader } from '@/components/ui/card'

export default function OrganizationsLoading() {
  return (
    <div className="container mx-auto py-8 px-4">
      <div className="space-y-6">
        {/* Header Skeleton */}
        <div className="flex justify-between items-center">
          <div className="space-y-2">
            <Skeleton className="h-8 w-32" />
            <Skeleton className="h-4 w-80" />
          </div>
          <Skeleton className="h-10 w-32" />
        </div>

        {/* Filters Card Skeleton */}
        <Card>
          <CardHeader>
            <Skeleton className="h-6 w-24" />
          </CardHeader>
          <CardContent>
            <div className="flex gap-4 items-center">
              <Skeleton className="h-10 flex-1" />
              <Skeleton className="h-10 w-48" />
            </div>
          </CardContent>
        </Card>

        {/* Table Skeleton */}
        <Card>
          <CardContent className="p-0">
            <div className="border rounded-lg">
              {/* Table Header */}
              <div className="border-b p-4">
                <div className="flex items-center space-x-4">
                  <Skeleton className="h-4 w-16" />
                  <Skeleton className="h-4 w-24" />
                  <Skeleton className="h-4 w-32" />
                  <Skeleton className="h-4 w-20" />
                  <Skeleton className="h-4 w-16" />
                  <Skeleton className="h-4 w-12" />
                </div>
              </div>

              {/* Table Rows */}
              {Array.from({ length: 5 }).map((_, index) => (
                <div key={index} className="border-b p-4 last:border-b-0">
                  <div className="flex items-center space-x-4">
                    <Skeleton className="h-10 w-10 rounded-full" />
                    <Skeleton className="h-4 w-32" />
                    <Skeleton className="h-4 w-48" />
                    <Skeleton className="h-4 w-20" />
                    <Skeleton className="h-6 w-16" />
                    <Skeleton className="h-8 w-8" />
                  </div>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>

        {/* Stats Cards Skeleton */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          {Array.from({ length: 3 }).map((_, index) => (
            <Card key={index}>
              <CardContent className="pt-6">
                <Skeleton className="h-8 w-12 mb-2" />
                <Skeleton className="h-3 w-16" />
              </CardContent>
            </Card>
          ))}
        </div>
      </div>
    </div>
  )
}