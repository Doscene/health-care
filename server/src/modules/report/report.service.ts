import { Injectable, Logger } from '@nestjs/common';
import { PrismaService } from '../../prisma/prisma.service.js';
import { FamilyService } from '../family/family.service.js';

@Injectable()
export class ReportService {
  private readonly logger = new Logger(ReportService.name);

  constructor(
    private readonly prisma: PrismaService,
    private readonly familyService: FamilyService,
  ) {}

  async generateWeeklyReport(familyId: string, weekStart: string) {
    const startDate = new Date(weekStart);
    const endDate = new Date(startDate);
    endDate.setDate(endDate.getDate() + 6);
    endDate.setHours(23, 59, 59, 999);

    const members = await this.familyService.getMembers(familyId);

    const memberSummaries = await Promise.all(
      members.map((member) =>
        this.getMemberWeeklySummary(member.userId, startDate, endDate),
      ),
    );

    const upcomingEvents = await this.getUpcomingEvents(familyId, members);

    return {
      id: `weekly-${familyId}-${weekStart}`,
      familyId,
      weekStart: startDate.toISOString().split('T')[0],
      weekEnd: endDate.toISOString().split('T')[0],
      members: memberSummaries,
      upcomingEvents,
      generatedAt: new Date().toISOString(),
    };
  }

  private async getMemberWeeklySummary(
    userId: string,
    startDate: Date,
    endDate: Date,
  ) {
    const user = await this.prisma.user.findUnique({
      where: { id: userId },
      select: { name: true, diseases: true },
    });

    const [bpRecords, bgRecords, medicationRecords] = await Promise.all([
      this.prisma.bloodPressureRecord.findMany({
        where: {
          userId,
          recordedAt: { gte: startDate, lte: endDate },
        },
        orderBy: { recordedAt: 'asc' },
      }),
      this.prisma.bloodSugarRecord.findMany({
        where: {
          userId,
          recordedAt: { gte: startDate, lte: endDate },
        },
        orderBy: { recordedAt: 'asc' },
      }),
      this.prisma.medicationRecord.findMany({
        where: {
          userId,
          scheduledTime: { gte: startDate, lte: endDate },
        },
        include: { medication: { select: { name: true } } },
      }),
    ]);

    const bpSummary = this.analyzeBloodPressure(bpRecords);
    const bgSummary = this.analyzeBloodSugar(bgRecords);
    const adherence = this.analyzeAdherence(
      medicationRecords,
      startDate,
      endDate,
    );

    const abnormalDays = this.calculateAbnormalDays(
      bpRecords,
      bgRecords,
      startDate,
      endDate,
    );
    const measurementCount = bpRecords.length + bgRecords.length;
    const missedCount = this.calculateMissedMeasurements(
      userId,
      startDate,
      endDate,
      measurementCount,
    );

    const { moodEmoji, summaryText, suggestion } = this.generateTextSummary(
      bpSummary,
      bgSummary,
      adherence,
      abnormalDays,
      user?.diseases,
    );

    return {
      userId,
      name: user?.name || '未知',
      disease: user?.diseases,
      moodEmoji,
      bpAvg: bpSummary.avg,
      bgAvg: bgSummary.avg,
      bpStatus: bpSummary.status,
      bgStatus: bgSummary.status,
      adherenceRate: adherence.rate,
      measurementCount,
      missedCount,
      abnormalDays,
      summaryText,
      suggestion,
    };
  }

  private analyzeBloodPressure(records: { systolic: number; diastolic: number }[]) {
    if (records.length === 0) {
      return { avg: null, status: 'normal' as const };
    }

    const systolicAvg =
      records.reduce((sum, r) => sum + r.systolic, 0) / records.length;
    const diastolicAvg =
      records.reduce((sum, r) => sum + r.diastolic, 0) / records.length;

    let status: 'normal' | 'elevated' | 'high' = 'normal';
    if (systolicAvg >= 140 || diastolicAvg >= 90) {
      status = 'high';
    } else if (systolicAvg >= 130 || diastolicAvg >= 85) {
      status = 'elevated';
    }

    return {
      avg: {
        systolic: Math.round(systolicAvg),
        diastolic: Math.round(diastolicAvg),
      },
      status,
    };
  }

  private analyzeBloodSugar(records: { value: number | { toString(): string } }[]) {
    if (records.length === 0) {
      return { avg: null, status: 'normal' as const };
    }

    const avg =
      records.reduce((sum, r) => sum + Number(r.value), 0) / records.length;

    let status: 'normal' | 'high' = 'normal';
    if (avg >= 7.0) {
      status = 'high';
    }

    return { avg: Math.round(avg * 10) / 10, status };
  }

  private analyzeAdherence(records: { status: string }[], startDate: Date, endDate: Date) {
    const totalScheduled = records.length;
    const taken = records.filter((r) => r.status === 'taken').length;

    const rate = totalScheduled > 0 ? Math.round((taken / totalScheduled) * 100) : 0;

    return { rate, taken, total: totalScheduled };
  }

  private calculateAbnormalDays(
    bpRecords: { systolic: number; diastolic: number; recordedAt: Date }[],
    bgRecords: { value: number | { toString(): string }; recordedAt: Date }[],
    startDate: Date,
    endDate: Date,
  ): number {
    const abnormalDays = new Set<string>();

    bpRecords.forEach((r) => {
      if (r.systolic >= 140 || r.diastolic >= 90) {
        abnormalDays.add(r.recordedAt.toISOString().split('T')[0]);
      }
    });

    bgRecords.forEach((r) => {
      if (Number(r.value) >= 7.0) {
        abnormalDays.add(r.recordedAt.toISOString().split('T')[0]);
      }
    });

    return abnormalDays.size;
  }

  private calculateMissedMeasurements(
    userId: string,
    startDate: Date,
    endDate: Date,
    actualCount: number,
  ): number {
    const days =
      Math.ceil(
        (endDate.getTime() - startDate.getTime()) / (1000 * 60 * 60 * 24),
      ) + 1;
    const expectedMeasurements = days * 2;
    return Math.max(0, expectedMeasurements - actualCount);
  }

  private generateTextSummary(
    bpSummary: { avg: { systolic: number; diastolic: number } | null; status: string },
    bgSummary: { avg: number | null; status: string },
    adherence: { rate: number; taken: number; total: number },
    abnormalDays: number,
    diseases: any,
  ) {
    let moodEmoji = '😊';
    let summaryText = '';
    let suggestion = '';

    if (bpSummary.avg) {
      const { systolic, diastolic } = bpSummary.avg;
      if (bpSummary.status === 'normal') {
        summaryText += `血压控制平稳，平均 ${systolic}/${diastolic} mmHg，均在目标范围内。`;
      } else if (bpSummary.status === 'elevated') {
        summaryText += `血压略偏高，平均 ${systolic}/${diastolic} mmHg，建议继续观察。`;
        moodEmoji = '🤔';
      } else {
        summaryText += `血压偏高需关注，平均 ${systolic}/${diastolic} mmHg，有多天超出目标值。`;
        moodEmoji = '😟';
        suggestion += '建议减少盐分摄入，保持规律作息。';
      }
    }

    if (bgSummary.avg) {
      if (bgSummary.status === 'normal') {
        summaryText += `血糖控制良好，平均 ${bgSummary.avg} mmol/L。`;
      } else {
        summaryText += `血糖偏高需关注，平均 ${bgSummary.avg} mmol/L。`;
        moodEmoji = '🤔';
        suggestion += '建议注意饮食，减少高糖食物摄入。';
      }
    }

    if (adherence.total > 0) {
      if (adherence.rate === 100) {
        summaryText += `${adherence.total}次服药全部按时完成，无漏服。`;
      } else {
        summaryText += `服药依从性 ${adherence.rate}%，有 ${adherence.total - adherence.taken} 次未按时服药。`;
        if (adherence.rate < 80) {
          moodEmoji = '😟';
          suggestion += '建议设置服药提醒，确保按时服药。';
        }
      }
    }

    if (abnormalDays > 0) {
      summaryText += `有 ${abnormalDays} 天出现异常指标。`;
    }

    if (!summaryText) {
      summaryText = '本周暂无记录数据，建议开始记录健康指标。';
    }

    if (!suggestion) {
      suggestion = '继续保持良好的健康习惯！';
    }

    return { moodEmoji, summaryText, suggestion };
  }

  async generateMonthlyReport(familyId: string, month: string) {
    const [year, mon] = month.split('-').map(Number);
    const startDate = new Date(year, mon - 1, 1);
    const endDate = new Date(year, mon, 0, 23, 59, 59, 999);

    const members = await this.familyService.getMembers(familyId);
    const memberIds = members.map((m) => m.userId);

    const weeklyData: { weekStart: Date; weekEnd: Date }[] = [];
    for (let i = 0; i < 4; i++) {
      const wStart = new Date(startDate);
      wStart.setDate(wStart.getDate() + i * 7);
      const wEnd = new Date(wStart);
      wEnd.setDate(wEnd.getDate() + 6);
      if (wEnd > endDate) wEnd.setTime(endDate.getTime());
      weeklyData.push({ weekStart: wStart, weekEnd: wEnd });
    }

    const memberSummaries = await Promise.all(
      members.map((member) =>
        this.getMemberMonthlySummary(member, weeklyData, startDate, endDate),
      ),
    );

    const [goals, appointments] = await Promise.all([
      this.prisma.familyGoal.findMany({
        where: {
          familyId,
          startDate: { lte: endDate },
          endDate: { gte: startDate },
        },
      }),
      this.prisma.appointment.findMany({
        where: {
          userId: { in: memberIds },
          date: { gte: startDate, lte: endDate },
        },
      }),
    ]);

    return {
      id: `monthly-${familyId}-${month}`,
      familyId,
      month,
      members: memberSummaries,
      goals: goals.map((g) => ({
        id: g.id,
        title: g.title,
        targetValue: g.targetValue,
        currentValue: g.currentValue,
        unit: g.unit,
        achieved: g.achieved,
        progress: g.targetValue > 0 ? Math.round((g.currentValue / g.targetValue) * 100) : 0,
      })),
      appointments: appointments.map((a) => ({
        userId: a.userId,
        department: a.department,
        hospital: a.hospital,
        date: a.date.toISOString().split('T')[0],
        status: a.status,
      })),
      generatedAt: new Date().toISOString(),
    };
  }

  private async getMemberMonthlySummary(
    member: { userId: string; name: string; diseases: any },
    weeklyData: { weekStart: Date; weekEnd: Date }[],
    startDate: Date,
    endDate: Date,
  ) {
    const [bpRecords, bgRecords, medicationRecords] = await Promise.all([
      this.prisma.bloodPressureRecord.findMany({
        where: { userId: member.userId, recordedAt: { gte: startDate, lte: endDate } },
        orderBy: { recordedAt: 'asc' },
      }),
      this.prisma.bloodSugarRecord.findMany({
        where: { userId: member.userId, recordedAt: { gte: startDate, lte: endDate } },
        orderBy: { recordedAt: 'asc' },
      }),
      this.prisma.medicationRecord.findMany({
        where: { userId: member.userId, scheduledTime: { gte: startDate, lte: endDate } },
      }),
    ]);

    const bpWeeklyTrend = weeklyData.map(({ weekStart, weekEnd }) => {
      const weekBp = bpRecords.filter(
        (r) => r.recordedAt >= weekStart && r.recordedAt <= weekEnd,
      );
      return this.analyzeBloodPressure(weekBp);
    });

    const bgWeeklyTrend = weeklyData.map(({ weekStart, weekEnd }) => {
      const weekBg = bgRecords.filter(
        (r) => r.recordedAt >= weekStart && r.recordedAt <= weekEnd,
      );
      return this.analyzeBloodSugar(weekBg);
    });

    const adherence = this.analyzeAdherence(medicationRecords, startDate, endDate);
    const abnormalDays = this.calculateAbnormalDays(bpRecords, bgRecords, startDate, endDate);

    const totalDays = Math.ceil(
      (endDate.getTime() - startDate.getTime()) / (1000 * 60 * 60 * 24),
    ) + 1;

    const daysWithRecords = new Set<string>();
    bpRecords.forEach((r) => daysWithRecords.add(r.recordedAt.toISOString().split('T')[0]));
    bgRecords.forEach((r) => daysWithRecords.add(r.recordedAt.toISOString().split('T')[0]));

    return {
      userId: member.userId,
      name: member.name,
      disease: member.diseases,
      bpAvg: this.analyzeBloodPressure(bpRecords).avg,
      bgAvg: this.analyzeBloodSugar(bgRecords).avg,
      bpWeeklyTrend: bpWeeklyTrend.map((t) => t.avg),
      bgWeeklyTrend: bgWeeklyTrend.map((t) => t.avg),
      adherenceRate: adherence.rate,
      totalDays,
      daysWithRecords: daysWithRecords.size,
      abnormalDays,
    };
  }

  async generateQuarterlyStory(userId: string, quarter: string) {
    const [year, q] = quarter.split('-Q').map(Number);
    const startMonth = (q - 1) * 3;
    const startDate = new Date(year, startMonth, 1);
    const endDate = new Date(year, startMonth + 3, 0, 23, 59, 59, 999);

    const prevStartDate = new Date(year, startMonth - 3, 1);
    const prevEndDate = new Date(year, startMonth, 0, 23, 59, 59, 999);

    const user = await this.prisma.user.findUnique({
      where: { id: userId },
      select: { name: true, diseases: true },
    });

    const [currentBp, prevBp, currentBg, prevBg, currentMeds, prevMeds] =
      await Promise.all([
        this.prisma.bloodPressureRecord.findMany({
          where: { userId, recordedAt: { gte: startDate, lte: endDate } },
        }),
        this.prisma.bloodPressureRecord.findMany({
          where: { userId, recordedAt: { gte: prevStartDate, lte: prevEndDate } },
        }),
        this.prisma.bloodSugarRecord.findMany({
          where: { userId, recordedAt: { gte: startDate, lte: endDate } },
        }),
        this.prisma.bloodSugarRecord.findMany({
          where: { userId, recordedAt: { gte: prevStartDate, lte: prevEndDate } },
        }),
        this.prisma.medicationRecord.findMany({
          where: { userId, scheduledTime: { gte: startDate, lte: endDate } },
        }),
        this.prisma.medicationRecord.findMany({
          where: { userId, scheduledTime: { gte: prevStartDate, lte: prevEndDate } },
        }),
      ]);

    const currentBpSummary = this.analyzeBloodPressure(currentBp);
    const prevBpSummary = this.analyzeBloodPressure(prevBp);
    const currentBgSummary = this.analyzeBloodSugar(currentBg);
    const prevBgSummary = this.analyzeBloodSugar(prevBg);

    const bpChange = currentBpSummary.avg && prevBpSummary.avg
      ? currentBpSummary.avg.systolic - prevBpSummary.avg.systolic
      : 0;

    const bgChange = currentBgSummary.avg && prevBgSummary.avg
      ? currentBgSummary.avg - prevBgSummary.avg
      : 0;

    const currentAdherence = this.analyzeAdherence(currentMeds, startDate, endDate);
    const prevAdherence = this.analyzeAdherence(prevMeds, prevStartDate, prevEndDate);

    const missedDetails = currentMeds
      .filter((m) => m.status === 'missed' || m.status === 'skipped')
      .map((m) => {
        const date = m.scheduledTime.toISOString().split('T')[0];
        return `${date} ${m.status === 'missed' ? '漏服' : '跳过'}`;
      });

    const nextGoal = this.generateQuarterGoal(currentBpSummary, currentBgSummary, currentAdherence);

    const quarterLabel = `${year}年${['春季', '夏季', '秋季', '冬季'][q - 1]}`;

    return {
      userId,
      name: user?.name || '未知',
      quarter: `Q${q} ${year}`,
      quarterLabel,
      bpTrend: {
        current: currentBpSummary.avg,
        previous: prevBpSummary.avg,
        change: bpChange,
        story: this.generateBpStory(bpChange, currentBpSummary),
      },
      bgTrend: {
        current: currentBgSummary.avg,
        previous: prevBgSummary.avg,
        change: bgChange,
        story: this.generateBgStory(bgChange, currentBgSummary),
      },
      adherence: {
        currentRate: currentAdherence.rate,
        previousRate: prevAdherence.rate,
        totalRecords: currentAdherence.total,
        missedCount: currentAdherence.total - currentAdherence.taken,
        missedDetails,
        story: this.generateAdherenceStory(currentAdherence, prevAdherence),
      },
      nextGoal,
      generatedAt: new Date().toISOString(),
    };
  }

  private generateBpStory(
    change: number,
    summary: { avg: { systolic: number; diastolic: number } | null; status: string },
  ): string {
    if (!summary.avg) return '本季度暂无血压记录。';

    const { systolic } = summary.avg;
    if (change < -5) {
      return `血压控制得非常好，平均收缩压比上季度下降了 ${Math.abs(change)} mmHg，继续保持！`;
    } else if (change > 5) {
      return `血压比上季度升高了 ${change} mmHg，需要注意饮食和作息。`;
    } else if (summary.status === 'normal') {
      return `血压稳定在 ${systolic} mmHg 左右，控制良好。`;
    } else {
      return `血压在 ${systolic} mmHg 附近波动，建议继续保持健康习惯。`;
    }
  }

  private generateBgStory(
    change: number,
    summary: { avg: number | null; status: string },
  ): string {
    if (!summary.avg) return '本季度暂无血糖记录。';

    if (change < -0.5) {
      return `血糖比上季度下降了 ${Math.abs(change).toFixed(1)} mmol/L，控制有进步！`;
    } else if (change > 0.5) {
      return `血糖比上季度升高了 ${change.toFixed(1)} mmol/L，建议注意饮食。`;
    } else if (summary.status === 'normal') {
      return `血糖稳定在 ${summary.avg} mmol/L，控制良好。`;
    } else {
      return `血糖在 ${summary.avg} mmol/L 附近，建议减少高糖食物摄入。`;
    }
  }

  private generateAdherenceStory(
    current: { rate: number; taken: number; total: number },
    prev: { rate: number; taken: number; total: number },
  ): string {
    const diff = current.rate - prev.rate;
    if (current.rate >= 95) {
      return `服药依从性 ${current.rate}%，表现优秀！`;
    } else if (diff > 5) {
      return `服药依从性从 ${prev.rate}% 提升到 ${current.rate}%，进步明显！`;
    } else if (current.rate < 80) {
      return `服药依从性 ${current.rate}%，漏服 ${current.total - current.taken} 次，建议设置提醒。`;
    } else {
      return `服药依从性 ${current.rate}%，继续保持。`;
    }
  }

  private generateQuarterGoal(
    bpSummary: { avg: { systolic: number; diastolic: number } | null; status: string },
    bgSummary: { avg: number | null; status: string },
    adherence: { rate: number },
  ): string {
    const goals: string[] = [];

    if (bpSummary.avg) {
      if (bpSummary.status !== 'normal') {
        goals.push(`平均收缩压控制在 130 mmHg 以下`);
      } else {
        goals.push(`保持平均收缩压 ${bpSummary.avg.systolic} mmHg`);
      }
    }

    if (bgSummary.avg && bgSummary.status !== 'normal') {
      goals.push(`平均空腹血糖控制在 7.0 mmol/L 以下`);
    }

    if (adherence.rate < 95) {
      goals.push(`服药依从性提升至 95% 以上`);
    }

    return goals.length > 0 ? goals.join('；') : '继续保持当前的健康状态';
  }

  async exportReport(
    userId: string,
    startDate: string,
    endDate: string,
    format: string,
  ) {
    const start = new Date(startDate);
    const end = new Date(endDate);
    end.setHours(23, 59, 59, 999);

    const [user, bpRecords, bgRecords, medicationRecords] = await Promise.all([
      this.prisma.user.findUnique({
        where: { id: userId },
        select: { name: true, phone: true, diseases: true },
      }),
      this.prisma.bloodPressureRecord.findMany({
        where: { userId, recordedAt: { gte: start, lte: end } },
        orderBy: { recordedAt: 'asc' },
      }),
      this.prisma.bloodSugarRecord.findMany({
        where: { userId, recordedAt: { gte: start, lte: end } },
        orderBy: { recordedAt: 'asc' },
      }),
      this.prisma.medicationRecord.findMany({
        where: { userId, scheduledTime: { gte: start, lte: end } },
        include: { medication: { select: { name: true } } },
      }),
    ]);

    const bpSummary = this.analyzeBloodPressure(bpRecords);
    const bgSummary = this.analyzeBloodSugar(bgRecords);
    const adherence = this.analyzeAdherence(medicationRecords, start, end);

    const bpHigh = bpRecords.filter((r) => r.systolic >= 140 || r.diastolic >= 90);
    const bgHigh = bgRecords.filter((r) => Number(r.value) >= 7.0);

    const htmlTemplate = this.generateExportHtml(
      user,
      start,
      end,
      bpRecords,
      bgRecords,
      medicationRecords,
      bpSummary,
      bgSummary,
      adherence,
      bpHigh.length,
      bgHigh.length,
    );

    return {
      html: htmlTemplate,
      format,
      fileName: `健康报告_${user?.name || ''}_${startDate}_${endDate}.pdf`,
      metadata: {
        userName: user?.name,
        startDate,
        endDate,
        bpRecordCount: bpRecords.length,
        bgRecordCount: bgRecords.length,
        medicationRecordCount: medicationRecords.length,
        adherenceRate: adherence.rate,
      },
    };
  }

  private generateExportHtml(
    user: any,
    startDate: Date,
    endDate: Date,
    bpRecords: any[],
    bgRecords: any[],
    medicationRecords: any[],
    bpSummary: any,
    bgSummary: any,
    adherence: any,
    bpHighCount: number,
    bgHighCount: number,
  ): string {
    const startStr = startDate.toISOString().split('T')[0];
    const endStr = endDate.toISOString().split('T')[0];

    const bpRows = bpRecords
      .map(
        (r) =>
          `<tr><td>${r.recordedAt.toISOString().split('T')[0]}</td><td>${r.systolic}/${r.diastolic}</td><td>${r.heartRate || '-'}</td></tr>`,
      )
      .join('');

    const bgRows = bgRecords
      .map(
        (r) =>
          `<tr><td>${r.recordedAt.toISOString().split('T')[0]}</td><td>${r.type}</td><td>${r.value}</td></tr>`,
      )
      .join('');

    const medRows = medicationRecords
      .map(
        (r) =>
          `<tr><td>${r.scheduledTime.toISOString().split('T')[0]}</td><td>${r.medication?.name || '-'}</td><td>${r.status === 'taken' ? '已服' : r.status === 'missed' ? '漏服' : r.status === 'skipped' ? '跳过' : '待服'}</td></tr>`,
      )
      .join('');

    return `<!DOCTYPE html>
<html><head><meta charset="UTF-8"><style>
body{font-family:sans-serif;margin:40px;color:#333}
h1{text-align:center;color:#2c5282}
h2{color:#2d3748;border-bottom:2px solid #e2e8f0;padding-bottom:5px}
table{width:100%;border-collapse:collapse;margin:10px 0}
th,td{border:1px solid #e2e8f0;padding:8px;text-align:left;font-size:12px}
th{background:#f7fafc}
.summary{background:#f0fff4;padding:15px;border-radius:8px;margin:10px 0}
.warning{background:#fffaf0;border-left:4px solid #ed8936;padding:10px;margin:10px 0}
</style></head><body>
<h1>家庭健康报告</h1>
<p style="text-align:center">姓名: ${user?.name || '未知'} | 时间: ${startStr} 至 ${endStr}</p>

<h2>血压统计</h2>
<div class="summary">
<p>平均: ${bpSummary.avg ? `${bpSummary.avg.systolic}/${bpSummary.avg.diastolic} mmHg` : '无数据'}</p>
<p>记录次数: ${bpRecords.length} | 偏高次数: ${bpHighCount}</p>
</div>
<table><tr><th>日期</th><th>血压(mmHg)</th><th>心率</th></tr>${bpRows}</table>

<h2>血糖统计</h2>
<div class="summary">
<p>平均: ${bgSummary.avg ? `${bgSummary.avg} mmol/L` : '无数据'}</p>
<p>记录次数: ${bgRecords.length} | 偏高次数: ${bgHighCount}</p>
</div>
<table><tr><th>日期</th><th>类型</th><th>值(mmol/L)</th></tr>${bgRows}</table>

<h2>服药记录</h2>
<div class="summary">
<p>依从率: ${adherence.rate}% | 按时: ${adherence.taken}/${adherence.total}</p>
</div>
<table><tr><th>日期</th><th>药品</th><th>状态</th></tr>${medRows}</table>

<p style="text-align:center;color:#718096;margin-top:40px;font-size:11px">
此报告仅供参考，不构成医疗建议。如有疑问请咨询医生。</p>
</body></html>`;
  }

  async generateShareCard(familyId: string, weekStart: string) {
    const report = await this.generateWeeklyReport(familyId, weekStart);
    const firstMember = report.members[0];

    const htmlTemplate = `<!DOCTYPE html>
<html><head><meta charset="UTF-8"><style>
body{width:375px;margin:0;padding:20px;font-family:sans-serif;background:linear-gradient(135deg,#667eea 0%,#764ba2 100%)}
.card{background:white;border-radius:16px;padding:24px;box-shadow:0 4px 12px rgba(0,0,0,0.15)}
.header{text-align:center;margin-bottom:20px}
.emoji{font-size:48px}
.title{font-size:18px;font-weight:bold;color:#2d3748;margin:8px 0}
.period{font-size:12px;color:#718096}
.content{font-size:14px;line-height:1.6;color:#4a5568}
.footer{text-align:center;margin-top:16px;font-size:11px;color:#a0aec0}
</style></head><body>
<div class="card">
<div class="header">
<div class="emoji">${firstMember?.moodEmoji || '😊'}</div>
<div class="title">上周健康简报</div>
<div class="period">${report.weekStart} 至 ${report.weekEnd}</div>
</div>
<div class="content">
<p><strong>${firstMember?.name || '家人'}</strong></p>
<p>${firstMember?.summaryText || '暂无数据'}</p>
${firstMember?.suggestion ? `<p style="color:#2b6cb0">💡 ${firstMember.suggestion}</p>` : ''}
</div>
<div class="footer">家庭慢病健康管理 · 仅供参考</div>
</div>
</body></html>`;

    return {
      html: htmlTemplate,
      reportId: report.id,
      memberName: firstMember?.name,
      moodEmoji: firstMember?.moodEmoji,
      summaryText: firstMember?.summaryText,
    };
  }

  private async getUpcomingEvents(familyId: string, members: { userId: string; name: string }[]) {
    const now = new Date();
    const twoWeeksLater = new Date(now);
    twoWeeksLater.setDate(twoWeeksLater.getDate() + 14);

    const memberIds = members.map((m) => m.userId);

    const appointments = await this.prisma.appointment.findMany({
      where: {
        userId: { in: memberIds },
        date: { gte: now, lte: twoWeeksLater },
        status: 'upcoming',
      },
      orderBy: { date: 'asc' },
    });

    return appointments.map((apt) => {
      const member = members.find((m) => m.userId === apt.userId);
      return {
        type: 'appointment',
        memberId: apt.userId,
        memberName: member?.name || '未知',
        title: `${apt.department}复诊`,
        date: apt.date.toISOString().split('T')[0],
      };
    });
  }
}
