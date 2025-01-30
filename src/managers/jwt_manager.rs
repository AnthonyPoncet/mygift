use jsonwebtoken::{decode, encode, Algorithm, DecodingKey, EncodingKey, Header, Validation};
use rsa::pkcs1::LineEnding;
use rsa::pkcs8::{EncodePrivateKey, EncodePublicKey};
use rsa::{RsaPrivateKey, RsaPublicKey};
use serde::{Deserialize, Serialize};
use std::collections::HashSet;

pub struct JwtManager {
    pub private_key: Vec<u8>,
    pub public_key: Vec<u8>,
    pub validation: Validation,
}

#[derive(thiserror::Error, Debug)]
#[error(transparent)]
pub(crate) enum JwtManagerError {
    JwtError(#[from] jsonwebtoken::errors::Error),
}

impl Default for JwtManager {
    fn default() -> Self {
        let mut rng = rand::thread_rng();
        let bits = 2048;
        let private_key = RsaPrivateKey::new(&mut rng, bits).unwrap();
        let public_key = RsaPublicKey::from(&private_key);
        let private_key_bytes = private_key
            .to_pkcs8_pem(LineEnding::LF)
            .unwrap()
            .as_bytes()
            .to_vec();
        let public_key_bytes = public_key
            .to_public_key_pem(LineEnding::LF)
            .unwrap()
            .as_bytes()
            .to_vec();

        let mut validation = Validation::new(Algorithm::RS256);
        validation.required_spec_claims = HashSet::new();
        validation.validate_exp = false;

        JwtManager {
            private_key: private_key_bytes,
            public_key: public_key_bytes,
            validation,
        }
    }
}

#[derive(Debug, Serialize, Deserialize)]
pub struct Claims {
    pub id: i64,
}

impl JwtManager {
    pub(crate) fn generate_jwt(&self, user_id: i64) -> Result<String, JwtManagerError> {
        let claims = Claims { id: user_id };
        Ok(encode(
            &Header::new(Algorithm::RS256),
            &claims,
            &EncodingKey::from_rsa_pem(&self.private_key).unwrap(),
        )?)
    }

    pub(crate) fn verify_jwt(&self, token: &str) -> Result<Claims, JwtManagerError> {
        let token = decode::<Claims>(
            token,
            &DecodingKey::from_rsa_pem(&self.public_key).unwrap(),
            &self.validation,
        )?;
        Ok(token.claims)
    }
}

#[cfg(test)]
mod test {
    use crate::managers::jwt_manager::JwtManager;

    #[test]
    fn test_check_token() {
        let jwt_manager = JwtManager::default();

        let token = jwt_manager.generate_jwt(1);
        let claims = jwt_manager.verify_jwt(&token.unwrap());
        assert_eq!(claims.unwrap().id, 1);
    }
}
