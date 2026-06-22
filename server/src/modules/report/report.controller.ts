import { Controller, Get, Query, UseGuards } from '@nestjs/common';
import { ApiTags, ApiOperation, ApiQuery } from '@nestjs/swagger';
import { ReportService } from './report.service.js';
import { JwtAuthGuard } from '../../common/guards/jwt-auth.guard.js';

@ApiTags('reports')
@Controller('api/reports')
export class ReportController {
  constructor(private readonly reportService: ReportService) {}

  @Get('weekly')
  @UseGuards(JwtAuthGuard)
  @ApiOperation({ summary: '生成周报' })
  @ApiQuery({ name: 'familyId', required: true })
  @ApiQuery({ name: 'weekStart', required: true, description: 'YYYY-MM-DD格式' })
  async getWeeklyReport(
    @Query('familyId') familyId: string,
    @Query('weekStart') weekStart: string,
  ) {
    return this.reportService.generateWeeklyReport(familyId, weekStart);
  }

  @Get('monthly')
  @UseGuards(JwtAuthGuard)
  @ApiOperation({ summary: '生成月报（双人对比）' })
  @ApiQuery({ name: 'familyId', required: true })
  @ApiQuery({ name: 'month', required: true, description: 'YYYY-MM格式' })
  async getMonthlyReport(
    @Query('familyId') familyId: string,
    @Query('month') month: string,
  ) {
    return this.reportService.generateMonthlyReport(familyId, month);
  }

  @Get('quarterly-story')
  @UseGuards(JwtAuthGuard)
  @ApiOperation({ summary: '生成季度故事报告' })
  @ApiQuery({ name: 'userId', required: true })
  @ApiQuery({ name: 'quarter', required: true, description: 'YYYY-QN格式，如2024-Q2' })
  async getQuarterlyStory(
    @Query('userId') userId: string,
    @Query('quarter') quarter: string,
  ) {
    return this.reportService.generateQuarterlyStory(userId, quarter);
  }

  @Get('export')
  @UseGuards(JwtAuthGuard)
  @ApiOperation({ summary: '导出复诊数据报告' })
  @ApiQuery({ name: 'userId', required: true })
  @ApiQuery({ name: 'startDate', required: true, description: 'YYYY-MM-DD格式' })
  @ApiQuery({ name: 'endDate', required: true, description: 'YYYY-MM-DD格式' })
  @ApiQuery({ name: 'format', required: false, description: 'pdf 或 image', enum: ['pdf', 'image'] })
  async exportReport(
    @Query('userId') userId: string,
    @Query('startDate') startDate: string,
    @Query('endDate') endDate: string,
    @Query('format') format: string,
  ) {
    return this.reportService.exportReport(userId, startDate, endDate, format || 'pdf');
  }

  @Get('share-card')
  @UseGuards(JwtAuthGuard)
  @ApiOperation({ summary: '生成分享图文卡片' })
  @ApiQuery({ name: 'familyId', required: true })
  @ApiQuery({ name: 'weekStart', required: true, description: 'YYYY-MM-DD格式' })
  async getShareCard(
    @Query('familyId') familyId: string,
    @Query('weekStart') weekStart: string,
  ) {
    return this.reportService.generateShareCard(familyId, weekStart);
  }
}
