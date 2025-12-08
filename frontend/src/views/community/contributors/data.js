// src/views/community/contributors/data.js
import avatar1 from '@/assets/images/avatars/avatar-1.png'
import avatar_fangsuyu from '@/assets/images/avatars/avatar-fangsuyu.png'
import cqcvc_cw_sr from '@/assets/images/cqcvc-cw-sr.jpg'

export const contributors = [
  // --- 核心团队 ---
  {
    name: 'FangSuYu',
    role: 'Founder & Lead Developer',
    category: 'core', // 核心团队
    avatar: avatar_fangsuyu,
    desc: 'OrangeTools 项目发起人，全栈开发者。致力于用技术改变校园生活。',
    tags: ['Vue3', 'Spring Boot', 'Architecture']
  },
  {
    name: 'Gemini',
    role: 'AI Co-pilot',
    category: 'core',
    avatar: 'https://www.gstatic.com/lamda/images/gemini_sparkle_v002_d4735304ff6292a690345.svg',
    desc: '来自 Google 的大型语言模型，全程辅助代码编写、架构设计与 Bug 修复。',
    tags: ['AI', 'LLM', 'Pair Programming']
  },

  // --- 社区贡献 ---
  {
    name: 'Early Adopters',
    role: '特别鸣谢',
    category: 'community', // 社区
    avatar: 'https://api.dicebear.com/7.x/identicon/svg?seed=Thanks',
    desc: '感谢每一位早期测试用户提供的宝贵课表数据与反馈。',
    tags: ['Testing', 'Feedback']
  },
  {
    name: 'XiaoJingYi',
    role: '特别鸣谢',
    category: 'community', // 社区
    avatar: avatar1,
    desc: 'OrangeTools项目图片、音频等素材提供者。',
    tags: ['Media Assets', 'Design Support', 'Creative']
  },
  {
    name: '城职表白墙',
    role: '校园合作伙伴',
    category: 'community', // 社区
    avatar: cqcvc_cw_sr,
    desc: '作为校园内影响力广泛的组织，与 OrangeTools 建立合作关系，协助在校内进行平台宣传与推广，为项目的发展带来了重要助力。联系方式：QQ 3627753823。',
    tags: ['Cooperation', 'Promotion', 'Campus Influence']
  }

]
