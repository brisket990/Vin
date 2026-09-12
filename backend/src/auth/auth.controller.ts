import { Body, Controller, Get, HttpCode, Post } from '@nestjs/common';
import { AuthService } from './auth.service.js';
import { RegisterHouseholdDto } from './dto/register-household.dto.js';
import { JoinHouseholdDto } from './dto/join-household.dto.js';
import { LoginDto } from './dto/login.dto.js';
import { CurrentUser } from '../common/decorators/current-user.decorator.js';
import { Public } from '../common/decorators/public.decorator.js';
import type { AuthenticatedUser } from './types.js';

@Controller('auth')
export class AuthController {
  constructor(private readonly authService: AuthService) {}

  @Public()
  @Post('register-household')
  registerHousehold(@Body() dto: RegisterHouseholdDto) {
    return this.authService.registerHousehold(dto);
  }

  @Public()
  @Post('join-household')
  joinHousehold(@Body() dto: JoinHouseholdDto) {
    return this.authService.joinHousehold(dto);
  }

  @Public()
  @HttpCode(200)
  @Post('login')
  login(@Body() dto: LoginDto) {
    return this.authService.login(dto);
  }

  @Get('me')
  me(@CurrentUser() user: AuthenticatedUser) {
    return user;
  }
}
