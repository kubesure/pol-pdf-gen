# build



# setup Dev

1. Build function zip
    
```
    gradle clean build -x test
``` 

2. Install AWS CLI and configure aws cli with dev admin key and secret

```
    pip3 install --upgrade --user awscli
    aws configure 
```

4. Create S3 bucket, permission & folder - if not done

```
    aws s3api create-bucket --bucket=io.kubesure-esyhealth-policy-issued-dev --region us-east-1

    aws s3api put-public-access-block --bucket io.kubesure-esyhealth-policy-issued-dev \
    --public-access-block-configuration BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true

    aws s3api put-object --bucket io.kubesure-esyhealth-policy-issued-dev --key unprocessed/blank.txt
    aws s3api put-object --bucket io.kubesure-esyhealth-policy-issued-dev --key processed/blank.txt    
```

2. Create lambda exection role 'lambda_s3_fullaccess' add policies s3 full & Lambda access - if not done

```
    cat <<eof > role_lambda.json 
    {
        "Version": "2012-10-17",
            "Statement": [
            {
                "Effect": "Allow",
                    "Principal": {
                    "Service": "lambda.amazonaws.com"
                },
            "Action": "sts:AssumeRole"
            }
        ]
    }

    eof
```

### Use admin role

```
    IAM_ROLE_ARN_LAMBDA=`aws iam create-role \
    --profile dev_admin \
 	--role-name "lambda_s3_fullaccess" \
 	--assume-role-policy-document file://role_lambda.json | jq -r .Role.Arn`
``` 

```
    aws iam attach-role-policy \
    --profile dev_admin  \
 	--role-name "lambda_s3_fullaccess" \
 	--policy-arn arn:aws:iam::aws:policy/AmazonS3FullAccess

    aws iam attach-role-policy \
    --profile dev_admin      \
 	--role-name "lambda_s3_fullaccess" \
 	--policy-arn arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole
```

3. Create Lambda Function

```
    FUNCTION_ARN=`aws lambda create-function --function-name esyhealth-pol-issued-pdf-gen \
    --zip-file fileb://build//distributions//pol-pdf-gen.zip \
    --handler kubesure.io.pol.pdf.gen.PolicyPDFGeneratorHandler::handleRequest \
    --runtime java8 \
    --role arn:aws:iam::708908412990:role/lambda_s3_fullaccess \
    --description "Generate policy pdf from html template" \
    --timeout 30 \
    --memory-size 256 | jq -r .FunctionArn`  

    aws lambda delete-function --function-name esyhealth-pol-issued-pdf-gen
```

4. Create S3 permission to invoke lambda function on PUT action

```
    aws lambda add-permission --function-name esyhealth-pol-issued-pdf-gen \
    --action lambda:InvokeFunction \
    --statement-id s3-account \
    --principal s3.amazonaws.com \
    --source-arn arn:aws:s3:::io.kubesure-esyhealth-policy-issued-dev \
    --source-account 708908412990
```

5. Create S3 trigger/notification for lambda function. Replace ARN from step 3

```
    aws s3api put-bucket-notification-configuration \
    --bucket io.kubesure-esyhealth-policy-issued-dev \
    --notification-configuration file://s3-notification.json    
```

6. Invoke/Test function

```
    aws lambda invoke   \
    --function-name esyhealth-pol-issued-pdf-gen \
    --payload file://test.json response.json

    aws lambda invoke \
    --function-name esyhealth-pol-issued-pdf-gen \
    --log-type Tail log.txt \
    --payload file://test.json \
    --query 'LogResult' \
    --output text |  base64 -d
```