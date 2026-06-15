import { Injectable, NotFoundException } from '@nestjs/common';
import { PrismaService } from '../../prisma/prisma.service.js';

@Injectable()
export class UserService {
  constructor(private readonly prisma: PrismaService) {}

  async getProfile(userId: string) {
    const user = await this.prisma.user.findUnique({
      where: { id: userId },
      select: {
        id: true,
        phone: true,
        name: true,
        avatar: true,
        selfRole: true,
        age: true,
        diseases: true,
        createdAt: true,
      },
    });
    if (!user) {
      throw new NotFoundException('用户不存在');
    }
    return user;
  }

  async updateRole(userId: string, selfRole: string, diseases: string[] = []) {
    return this.prisma.user.update({
      where: { id: userId },
      data: { selfRole, diseases },
      select: {
        id: true,
        phone: true,
        name: true,
        avatar: true,
        selfRole: true,
        age: true,
        diseases: true,
      },
    });
  }

  async updateProfile(
    userId: string,
    data: { name?: string; avatar?: string; age?: number },
  ) {
    return this.prisma.user.update({
      where: { id: userId },
      data,
      select: {
        id: true,
        phone: true,
        name: true,
        avatar: true,
        selfRole: true,
        age: true,
        diseases: true,
      },
    });
  }
}
