import { Injectable, NotFoundException } from '@nestjs/common';
import { PrismaService } from '../../prisma/prisma.service.js';
import { v4 as uuidv4 } from 'uuid';

@Injectable()
export class AppointmentService {
  constructor(private readonly prisma: PrismaService) {}

  /** 获取复诊计划列表 */
  async getPlans(userId: string) {
    const plans = await this.prisma.appointment.findMany({
      where: { userId },
      orderBy: { date: 'asc' },
    });

    return plans.map((p) => ({
      id: p.id,
      hospital: p.hospital,
      department: p.department,
      date: p.date.toISOString().split('T')[0],
      reminderDays: p.remindBefore,
      status: p.status,
      notes: p.notes,
    }));
  }

  /** 添加复诊计划 */
  async addPlan(
    userId: string,
    data: {
      hospital: string;
      department: string;
      date: string;
      reminderDays?: number;
      notes?: string;
    },
  ) {
    const plan = await this.prisma.appointment.create({
      data: {
        id: uuidv4(),
        userId,
        hospital: data.hospital,
        department: data.department,
        date: new Date(data.date),
        remindBefore: data.reminderDays ?? 3,
        notes: data.notes,
        status: 'upcoming',
      },
    });

    return {
      id: plan.id,
      hospital: plan.hospital,
      department: plan.department,
      date: plan.date.toISOString().split('T')[0],
      reminderDays: plan.remindBefore,
      status: plan.status,
      notes: plan.notes,
    };
  }

  /** 删除复诊计划 */
  async deletePlan(userId: string, planId: string) {
    const plan = await this.prisma.appointment.findFirst({
      where: { id: planId, userId },
    });

    if (!plan) {
      throw new NotFoundException('复诊计划不存在');
    }

    await this.prisma.appointment.delete({ where: { id: planId } });
    return null;
  }
}
