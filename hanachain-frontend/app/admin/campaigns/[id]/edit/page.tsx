"use client";

import { useState, useEffect } from "react";
import { useParams, useRouter } from "next/navigation";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { ArrowLeft, Loader2 } from "lucide-react";
import { AdminCampaignEditForm } from "@/components/admin/campaign-edit-form";
import { getAdminCampaignDetail } from "@/lib/api/campaign-api";
import { handleApiError } from "@/lib/error-handler";
import { toast } from "sonner";
import type { CampaignDetailItem } from "@/types/donation";

export default function AdminCampaignEditPage() {
  const params = useParams();
  const router = useRouter();
  const [campaign, setCampaign] = useState<CampaignDetailItem | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const campaignId = params?.id ? parseInt(params.id as string) : null;

  useEffect(() => {
    if (!campaignId) {
      setError("유효하지 않은 캠페인 ID입니다.");
      setLoading(false);
      return;
    }

    loadCampaign();
  }, [campaignId]);

  const loadCampaign = async () => {
    if (!campaignId) return;

    try {
      setLoading(true);
      setError(null);
      const data = await getAdminCampaignDetail(campaignId);
      setCampaign(data);
    } catch (err) {
      const apiError = handleApiError(err);
      setError(apiError.message);
      toast.error(apiError.message);
    } finally {
      setLoading(false);
    }
  };

  const handleUpdateSuccess = () => {
    toast.success("캠페인이 성공적으로 수정되었습니다.");
    router.push(`/admin/campaigns/${campaignId}`);
  };

  const handleBack = () => {
    router.back();
  };

  if (loading) {
    return (
      <div className="container mx-auto p-6">
        <div className="flex items-center justify-center min-h-[400px]">
          <div className="flex items-center space-x-2">
            <Loader2 className="h-6 w-6 animate-spin" />
            <span>캠페인 정보를 불러오고 있습니다...</span>
          </div>
        </div>
      </div>
    );
  }

  if (error || !campaign) {
    return (
      <div className="container mx-auto p-6">
        <Card>
          <CardContent className="flex items-center justify-center min-h-[200px]">
            <div className="text-center space-y-2">
              <p className="text-destructive">{error || "캠페인을 찾을 수 없습니다."}</p>
              <Button onClick={handleBack} variant="outline">
                돌아가기
              </Button>
            </div>
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className="container mx-auto p-6 space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center space-x-4">
          <Button 
            onClick={handleBack} 
            variant="outline" 
            size="sm"
          >
            <ArrowLeft className="h-4 w-4 mr-2" />
            돌아가기
          </Button>
          <div>
            <h1 className="text-2xl font-bold">캠페인 수정</h1>
          </div>
        </div>
      </div>

      {/* Edit Form */}
      <Card>
        <CardHeader>
          <CardTitle>캠페인 정보 수정</CardTitle>
        </CardHeader>
        <CardContent>
          <AdminCampaignEditForm 
            campaign={campaign}
            onSuccess={handleUpdateSuccess}
          />
        </CardContent>
      </Card>
    </div>
  );
}