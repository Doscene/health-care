import { Controller, Get } from '@nestjs/common';
import { ApiTags } from '@nestjs/swagger';
import { Public } from './common/decorators/public.decorator';

@ApiTags('Health')
@Controller()
export class AppController {
  @Public()
  @Get()
  getHello(): { status: string; version: string } {
    return {
      status: 'ok',
      version: '1.0.0',
    };
  }

  @Public()
  @Get('health')
  getHealth(): { status: string; version: string } {
    return {
      status: 'ok',
      version: '1.0.0',
    };
  }
}
