/** @type {import('next').NextConfig} */
const nextConfig = {
  eslint: {
    ignoreDuringBuilds: true,
  },
  typescript: {
    ignoreBuildErrors: true,
  },
  images: {
    unoptimized: true,
  },
  // dev indicator 완전 비활성화
  devIndicators: {
    buildActivity: false,
    buildActivityPosition: 'bottom-right',
  },
  // 에러 오버레이 완전 비활성화
  webpack: (config, { dev, isServer }) => {
    if (dev && !isServer) {
      // 에러 오버레이 관련 모듈 제거
      config.resolve.alias = {
        ...config.resolve.alias,
        'next/dist/client/dev/error-overlay': false,
        'next/dist/client/components/react-dev-overlay': false,
        'next/dist/client/dev/error-overlay.js': false,
      };
      
      // 에러 오버레이 플러그인들 제거
      config.plugins = config.plugins.filter(plugin => {
        const pluginName = plugin.constructor.name;
        return !pluginName.includes('ErrorOverlay') && 
               !pluginName.includes('ReactDevOverlay') &&
               !pluginName.includes('HotReloader') &&
               !pluginName.includes('BuildIndicator');
      });
      
      // dev indicator 관련 코드 제거
      config.module.rules.push({
        test: /next\/dist\/client\/dev\/error-overlay/,
        use: 'null-loader'
      });
    }
    return config;
  },
}

export default nextConfig
