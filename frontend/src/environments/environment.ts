export const environment = {
  production: true,
  api: {
    protocol: 'https',
    host: 'api.race-photos.example.com',
    port: 443,
    basePath: '/api'
  },
  auth: {
    region: 'us-east-1',
    userPoolId: 'us-east-1_production_pool',
    userPoolClientId: 'productionClientId',
    domain: 'your-prod-domain.auth.us-east-1.amazoncognito.com',
    redirectSignIn: 'https://app.race-photos.example.com/signin/callback',
    redirectSignOut: 'https://app.race-photos.example.com/',
    scopes: ['openid', 'email', 'profile']
  }
};
