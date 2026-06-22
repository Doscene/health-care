import { Injectable } from '@nestjs/common';
import { PrismaService } from '../../prisma/prisma.service.js';

@Injectable()
export class HomeService {
  constructor(private readonly prisma: PrismaService) {}

  /** 获取患者首页数据 */
  async getPatientHomeData(userId: string) {
    const now = new Date();
    const todayStart = new Date(
      now.getFullYear(),
      now.getMonth(),
      now.getDate(),
    );
    const todayEnd = new Date(todayStart.getTime() + 24 * 60 * 60 * 1000);

    // 并行查询
    const [
      user,
      latestBp,
      latestBg,
      todayMedications,
      activeAlerts,
      todayAppointments,
    ] = await Promise.all([
      // 用户信息
      this.prisma.user.findUnique({
        where: { id: userId },
        select: { name: true, selfRole: true, diseases: true },
      }),
      // 最近一条血压记录
      this.prisma.bloodPressureRecord.findFirst({
        where: { userId },
        orderBy: { recordedAt: 'desc' },
        select: {
          systolic: true,
          diastolic: true,
          heartRate: true,
          recordedAt: true,
        },
      }),
      // 最近一条血糖记录
      this.prisma.bloodSugarRecord.findFirst({
        where: { userId },
        orderBy: { recordedAt: 'desc' },
        select: { type: true, value: true, recordedAt: true },
      }),
      // 今日待服药计划
      this.prisma.medication.findMany({
        where: { userId, status: 'active' },
        select: {
          id: true,
          name: true,
          dosagePerTime: true,
          frequencyPerDay: true,
          timeSlots: true,
        },
      }),
      // 活跃风险预警
      this.prisma.riskAlert.findMany({
        where: { userId, status: 'active' },
        orderBy: { createdAt: 'desc' },
        take: 3,
        select: {
          id: true,
          level: true,
          triggerType: true,
          triggerValue: true,
          createdAt: true,
        },
      }),
      // 今日复诊计划
      this.prisma.appointment.findMany({
        where: {
          userId,
          date: { gte: todayStart, lt: todayEnd },
        },
        select: { id: true, hospital: true, department: true, date: true },
      }),
    ]);

    return {
      user: {
        name: user?.name ?? '用户',
        selfRole: user?.selfRole ?? 'patient',
        diseases: user?.diseases ?? [],
      },
      latestBp: latestBp
        ? {
            systolic: latestBp.systolic,
            diastolic: latestBp.diastolic,
            heartRate: latestBp.heartRate,
            recordedAt: latestBp.recordedAt,
          }
        : null,
      latestBg: latestBg
        ? {
            type: latestBg.type,
            value: Number(latestBg.value),
            recordedAt: latestBg.recordedAt,
          }
        : null,
      todayMedications: todayMedications.map((m) => ({
        id: m.id,
        name: m.name,
        dosagePerTime: m.dosagePerTime,
        frequencyPerDay: m.frequencyPerDay,
        timeSlots: m.timeSlots,
      })),
      activeAlerts: activeAlerts.map((a) => ({
        id: a.id,
        level: a.level,
        triggerType: a.triggerType,
        triggerValue: a.triggerValue,
        createdAt: a.createdAt,
      })),
      todayAppointments: todayAppointments.map((a) => ({
        id: a.id,
        hospital: a.hospital,
        department: a.department,
        date: a.date,
      })),
    };
  }

  /** 获取家庭成员健康概览（子女/照护者首页） */
  async getFamilyHomeData(userId: string) {
    // 获取用户所属的所有家庭
    const memberships = await this.prisma.familyMember.findMany({
      where: { userId },
      include: { family: true },
    });

    if (memberships.length === 0) {
      return { families: [] };
    }

    const familyIds = memberships.map((m) => m.familyId);

    // 获取所有家庭的成员（排除自己）
    const allMembers = await this.prisma.familyMember.findMany({
      where: {
        familyId: { in: familyIds },
        userId: { not: userId },
      },
      include: {
        user: {
          select: {
            id: true,
            name: true,
            selfRole: true,
            diseases: true,
          },
        },
      },
    });

    // 为每个成员获取最新健康数据
    const memberIds = allMembers.map((m) => m.userId);
    const [latestBps, latestBgs, activeAlerts] = await Promise.all([
      this.prisma.bloodPressureRecord.findMany({
        where: { userId: { in: memberIds } },
        orderBy: { recordedAt: 'desc' },
        distinct: ['userId'],
        select: {
          userId: true,
          systolic: true,
          diastolic: true,
          heartRate: true,
          recordedAt: true,
        },
      }),
      this.prisma.bloodSugarRecord.findMany({
        where: { userId: { in: memberIds } },
        orderBy: { recordedAt: 'desc' },
        distinct: ['userId'],
        select: {
          userId: true,
          type: true,
          value: true,
          recordedAt: true,
        },
      }),
      this.prisma.riskAlert.findMany({
        where: { userId: { in: memberIds }, status: 'active' },
        select: { userId: true, level: true, triggerType: true },
      }),
    ]);

    const bpMap = new Map(latestBps.map((b) => [b.userId, b]));
    const bgMap = new Map(latestBgs.map((b) => [b.userId, b]));
    const alertMap = new Map<string, typeof activeAlerts>();

    for (const alert of activeAlerts) {
      const list = alertMap.get(alert.userId) ?? [];
      list.push(alert);
      alertMap.set(alert.userId, list);
    }

    return {
      families: memberships.map((m) => ({
        familyId: m.familyId,
        familyName: m.family.name,
        myRole: m.role,
        members: allMembers
          .filter((am) => am.familyId === m.familyId)
          .map((am) => {
            const bp = bpMap.get(am.userId);
            const bg = bgMap.get(am.userId);
            const alerts = alertMap.get(am.userId) ?? [];
            return {
              userId: am.userId,
              name: am.user.name,
              selfRole: am.user.selfRole,
              diseases: am.user.diseases,
              latestBp: bp
                ? {
                    systolic: bp.systolic,
                    diastolic: bp.diastolic,
                    heartRate: bp.heartRate,
                  }
                : null,
              latestBg: bg ? { type: bg.type, value: Number(bg.value) } : null,
              activeAlertCount: alerts.length,
              hasHighAlert: alerts.some(
                (a) => a.level === 'high' || a.level === 'critical',
              ),
            };
          }),
      })),
    };
  }
}
