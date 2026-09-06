export type ViewType = 'LOGIN' | 'REGISTER' | 'HOME' | 'HISTORY' | 'APPLY' | 'CONFIRM' | 'SUCCESS' | 'PROFILE';

export interface FormData {
  amount: string;
  term: string;
  fullName: string;
  dob: string;
  gender: string;
  purpose: string;
  email: string;
  phone: string;
  cccd: string;
  address: string;
  city: string;
  district: string;
  ward: string;
  occupation: string;
  income: string;
  ec1Name: string;
  ec1Relation: string;
  ec1Phone: string;
  ec2Name: string;
  ec2Relation: string;
  ec2Phone: string;
}
