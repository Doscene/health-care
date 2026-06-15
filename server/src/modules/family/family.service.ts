import {
  Injectable,
  Logger,
  NotFoundException,
  BadRequestException,
  ConflictException,
} from '@nestjs/common';
import { PrismaService } from '../../prisma/prisma.service.js';
import { v4 as uuidv4 } from 'uuid';

@Injectable()
export class FamilyService {
  constructor(private readonly prisma: PrismaService) {}

  private readonly logger = new Logger(FamilyService.name);

  /** 允许加入家庭时使用的角色白名单 */
  private static readonly ALLOWED_JOIN_ROLES = ['member', 'caregiver', 'viewer'];

  /** 生成6位邀请码（带碰撞重试） */
  private async generateUniqueInviteCode(): Promise<string> {
    for (let attempt = 0; attempt < 5; attempt++) {
      const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
      let code = '';
      for (let i = 0; i < 6; i++) {
        code += chars.charAt(Math.floor(Math.random() * chars.length));
      }
      const existing = await this.prisma.family.findFirst({
        where: { inviteCode: code, inviteCodeExpire: { gt: new Date() } },
      });
      if (!existing) return code;
    }
    throw new Error('无法生成唯一邀请码，请重试');
  }

  /** 创建家庭 */
  async createFamily(userId: string, name: string) {
    if (!name || name.trim().length === 0) {
      throw new BadRequestException('家庭名称不能为空');
    }
    if (name.length > 50) {
      throw new BadRequestException('家庭名称不能超过50个字符');
    }

    const inviteCode = await this.generateUniqueInviteCode();
    const inviteCodeExpire = new Date(Date.now() + 48 * 60 * 60 * 1000);

    const family = await this.prisma.family.create({
      data: {
        id: uuidv4(),
        name,
        creatorId: userId,
        inviteCode,
        inviteCodeExpire,
        memberCount: 1,
        members: {
          create: {
            id: uuidv4(),
            userId,
            role: 'owner',
            visibility: {},
          },
        },
      },
      include: { members: true },
    });

    return {
      id: family.id,
      name: family.name,
      inviteCode: family.inviteCode,
      inviteCodeExpiresAt: family.inviteCodeExpire,
      memberCount: family.memberCount,
    };
  }

  /** 获取我的家庭列表 */
  async getMyFamilies(userId: string) {
    const memberships = await this.prisma.familyMember.findMany({
      where: { userId },
      include: {
        family: true,
      },
    });

    return memberships.map((m) => ({
      id: m.family.id,
      name: m.family.name,
      inviteCode: m.family.inviteCode,
      inviteCodeExpiresAt: m.family.inviteCodeExpire,
      memberCount: m.family.memberCount,
      myRole: m.role,
    }));
  }

  /** 验证邀请码 */
  async verifyInviteCode(code: string) {
    const family = await this.prisma.family.findFirst({
      where: {
        inviteCode: code.toUpperCase(),
        inviteCodeExpire: { gt: new Date() },
      },
    });

    if (!family) {
      throw new NotFoundException('邀请码无效或已过期');
    }

    return {
      familyName: family.name,
      memberCount: family.memberCount,
    };
  }

  /** 加入家庭 */
  async joinFamily(userId: string, code: string, role: string) {
    // 角色白名单校验
    const safeRole = FamilyService.ALLOWED_JOIN_ROLES.includes(role) ? role : 'member';

    const family = await this.prisma.family.findFirst({
      where: {
        inviteCode: code.toUpperCase(),
        inviteCodeExpire: { gt: new Date() },
      },
    });

    if (!family) {
      throw new NotFoundException('邀请码无效或已过期');
    }

    const existing = await this.prisma.familyMember.findUnique({
      where: {
        familyId_userId: {
          familyId: family.id,
          userId,
        },
      },
    });

    if (existing) {
      throw new ConflictException('您已是该家庭成员');
    }

    await this.prisma.familyMember.create({
      data: {
        id: uuidv4(),
        familyId: family.id,
        userId,
        role: safeRole,
        visibility: {},
      },
    });

    await this.prisma.family.update({
      where: { id: family.id },
      data: { memberCount: { increment: 1 } },
    });

    return { message: '加入成功', familyId: family.id };
  }

  /** 获取家庭成员列表 */
  async getMembers(familyId: string) {
    const members = await this.prisma.familyMember.findMany({
      where: { familyId },
      include: {
        user: {
          select: {
            id: true,
            name: true,
            avatar: true,
            selfRole: true,
            diseases: true,
          },
        },
      },
    });

    return members.map((m) => ({
      userId: m.user.id,
      name: m.user.name,
      avatar: m.user.avatar,
      role: m.role,
      selfRole: m.user.selfRole,
      diseases: m.user.diseases,
      joinedAt: m.joinedAt,
    }));
  }

  /** 重新生成邀请码 */
  async regenerateInviteCode(familyId: string) {
    const inviteCode = await this.generateUniqueInviteCode();
    const inviteCodeExpire = new Date(Date.now() + 48 * 60 * 60 * 1000);

    const family = await this.prisma.family.update({
      where: { id: familyId },
      data: { inviteCode, inviteCodeExpire },
    });

    return {
      inviteCode: family.inviteCode,
      inviteCodeExpiresAt: family.inviteCodeExpire,
    };
  }
}
