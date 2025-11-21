export const environment = {
  production: false,
  api: {
    protocol: 'http',
    host: 'localhost',
    port: 8080,
    basePath: '/api'
  },
  auth: {
    region: 'eu-central-1',
    userPoolId: 'eu-central-1_INZ1CsI17',
    userPoolClientId: '63q7he7avjhqcc3nnem016dd1u',
    domain: 'eu-central-1inz1csi17.auth.eu-central-1.amazoncognito.com',
    redirectSignIn: 'http://localhost:4200/signin/callback',
    redirectSignOut: 'http://localhost:4200/',
    scopes: ['openid', 'email', 'profile']
  }
};
