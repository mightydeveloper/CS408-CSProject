# 노타키보드
Updated 2017.12.12 by deNsuh

## Development
__Currently, any functionalities in NOTA application is tested with Samsung Galaxy S6 with Android SDK v.24__
Any behaviors on other SDK versions or machines are not guaranteed and tested.

### Prerequisites and Tools

이클립스를 사용해도 무방하나 이왕이면 안드로이드 스튜디오가 짱이다.

### Package Structure

```text
com.notakeyboard/
├── db/
├── experiment/
│   └── metric/
├── initialize/
│   └── adapter/
├── keyboard/
├── ml/
├── obj/
├── preference/
├── tensorflow/
└── util/
```
다음은 각 패키지의 대략적 설명이다. 클래스별 자세한 설명은 각 클래스의 javadoc을 참고.
- `db`: 키보드 실행에 필요한 각종 정보를 담고 있다. 사용자의 preference, 설정값, 실험 데이터, 키보드 레이아웃 데이터, 터치 기록 등.
- `experiment`: 현재 키보드 성능 실험을 위한 대부분의 코드가 이 패키지에 들어있다. Production에는 통째로 제거해도 좋다.
    - `metric`: 성능 메트릭 측정을 위한 기능적 클래스가 존재함
- `initialize`: 노타 사용법 설명 (처음 실행시 뜸)
    - `adapter`: 노타 사용법 설명 구현 helper
- `keyboard`: 키보드 클래스가 정의되어있다. Input Method Service는 사용자가 입력을 필요로 하는 순간에 생성되며, 이 클래스에서 키보드 인스턴스와 View를 생성한다.
- `ml`: 학습 관련 기능이 구현되어있다. 입력 키 값을 추론하거나 키보드를 학습시키는 주요 기능이 존재한다.
- `obj`: 키보드의 각종 object들에 대한 정의
- `preference`: 사용자 설정을 관리하는 클래스이며, 노타의 '메인 메뉴'가 구현되어있기도 하다.
- `tensorflow`: Language Model을 사용하기 위한 기능들이 구현되어있다.
- `util`: 노타 어플리케이션을 위해 특별히 정의된 기능성 클래스들이 있다.

추가적으로, 한글 오토마타는 `HangulAutomata`클래스에 정의되어있다.

### Assets
`assets` 폴더에는 language model의 frozen model과, 성능 측정 실험 용도로 사용되는 문장이 담긴 `pangrams.txt`가 존재한다

### Tensorflow in Android
Tensorflow 모델을 안드로이드에 임베딩 하기 위해서는 frozen model과 java api를 호출할 라이브러리가 있어야한다.
- 라이브러리는 `build.gradle` 파일의 `dependencies`에 `compile 'org.tensorflow:tensorflow-android:1.3.0'`를 추가하면 된다. [여기 참고](https://www.tensorflow.org/mobile/android_build)
- 직접 정의한 모델은 protobuf 형식으로 '얼려야' 안드로이드 프로그램에 임베딩할 수 있다. 방식은 [여기 참고](https://blog.metaflow.fr/tensorflow-how-to-freeze-a-model-and-serve-it-with-a-python-api-d4f3596b3adc)

### Code style and conventions
Use [Google's java programming style guide](https://google.github.io/styleguide/javaguide.html) for styles and javadoc.
Additionally, `PrintLog`를 사용한 디버깅 메세지를 사용할 경우에는 언제나 `TODO`로 디버깅 메세지를 나중에 제거하라는 메세지를 남기는 것이 좋다. IDE에서 찾기도 쉽고.

## 터치시퀀스 추론 기법

사용자가 어느 글자를 치기 위해 소프트키보드 위에 터치를 하면, 해당 터치의 소프트키보드상 좌표를 구할 수 있다.
터치시퀀스는 해당 좌표가 어느 글자 키(ㄱ, ㄴ, ㅏ, ㅐ, 이동 등)에 매핑되는지를 머신 러닝 및 딥러닝 모델을 활용하여 추론하는 알고리즘이다.

### 가우시안 혼합 분포 (Gaussian Mixture Model, GMM)를 이용한 1차 추론

사용자가 특정 좌표 (x, y)에 터치를 하면, 해당 터치가 유효한 터치인지 우선 판단한다.
좌표의 값이소프트키보드 영역을 벗어나있거나 사용자 터치들의 GMM이 학습되어있지 않다면 어떠한 추론 과정도 거치지 않은 기저 글자(base keycode)를 출력하게 된다.
기저 글자란 단순히 입력 좌표가 어느 글자 키 영역에 위치하는지만을 보고 해당 글자 키가 표현하는 글자를 의미한다.
유효한 터치 좌표가 발생했다면, 1차로 GMM을 통한 추론을 하게 된다.
GMM으로부터 해당 터치 좌표가 각각 어떤 키의 분포에 속하는지, 그리고 각 키의 분포 내에서 얼마나 유력한지를 계산하여 터치가 각각 키 영역(QWERTY 기준 44개 키)에 속해 있을 확률을 계산한다.
이렇게 얻은 확률 벡터에서 가장 값이 큰 키가 표현하는 글자는 'GMM 추론 글자(GMM-inferred keycode)'가 된다.
그러나 GMM 추론 글자가 가우시안 모드로부터 지나치게 멀리 떨어져 있다면 다시 기저 글자로 회귀하게 된다.
GMM 추론 과정은 보여지는 소프트키보드와 실제 입력 인식부 영역을 다르게 하여 사용자의 과거 터치 분포를 기반으로 새롭게 가장 높은 확률을 가지는 키를 도출하는 과정이다.

### 언어 모델 (Language Model, LM)을 이용한 2차 추론

이전 언어 모델의 최종 추론 결과는 언어 모델의 입력으로 주어 모델의 추론 과정을 한 번 거치게 된다.
언어 모델은 언어 모델을 기반으로 학습된 RNN 모델을 압축하여 어플리케이션 내에 삽입해놓은 모듈이다.
LM은 모든 키에 대한 GMM 확률 값을 입력받아 다음에 어떤 키가 입력될지를 판단하는 새로운 확률 분포를 출력한다.
이 출력 값을 기억하고 있다가 다음 터치를 입력받고, GMM 확률 벡터가 계산된 후에는 기억하고 있던 출력 값 벡터와 결과를 합산해 2차 추론값을 계산하게 된다.
합산하는 방법은 기본적으로는 이전 언어모델 출력 벡터와 GMM 확률 벡터를 요소별 곱(element-wise product)하는 방법을 사용하나, 각 요소별 weight를 조정하거나 벡터들을 sum-to-one, normalize, soften하는 등 전처리 과정을 거치면서 결과 값을 가장 잘 추론할 수 있도록 조정한다.
이 추론값들 중 가장 값이 큰 키가 표현하는 글자를 'LM 추론 글자(LM-inferred keycode)'가 된다.
그러나, 1차 추론 결과와 마찬가지로 LM 추론 글자 키가 키의 중심으로부터 지나치게 멀리 떨어져 있다면 출력값은 1차 추론 글자로 회귀하게 된다.
사용자는 최종 LM 추론 글자를 화면에서 보게 된다.

## 구현 이슈

### Nearest Keys Only for GMM Prediction

소프트 키보드 어플리케이션은 특정 시간 이내에 추론된 글자가 화면에 나타나지 않으면 사용자가 매우 크게 불편을 느끼는 hard real-time application의 특성을 가진다. 그렇기에 키보드 추론 과정에서 계산 시간을 줄이기 위한 여러 구현상 기법들이 포함되었다.
그 중 한가지 기법은 GMM 예측 계산 과정을 줄이는 기법이다.
GMM 추론 시 사용자가 대신 입력하려고 했던 키는 사용자가 입력한 좌표에 해당하는 키 주변에 위치할 것이다. 
만일 너무 멀리 떨어진 키를 최종 추론하게 되면 이는 사용자가 오히려 불편을 느낄 수 있는 원인이 된다.
또한, 실제로 가우시안 혼합 분포를 이용해 확률 값을 계산해 보면 입력한 키 주변에 있는 키 외에는 전부 확률값이 매우 낮게 나와 의미가 없어진다.
그렇기 때문에, GMM 추론 시 사용자가 입력한 키 주변에 있는 키들에 대한 확률 값만 계산하도록 하여 불필요한 계산 시간을 줄일 수 있다.
주변 키가 아닌 키들에 대한 확률 값은 0으로 조정된다.

### Sum To One On Final Prediction

언어 모델을 통한 2차 추론 과정에서 GMM으로부터 도출된 1차 확률 계산 벡터와, 이전 언어 모델의 예측 벡터 결과를 합산한 이후의 벡터는 그 합이 1임을 보장받지 못한다. 따라서 합산 결과 벡터의 각 요소가 합이 1이 되도록 각 요소에 일정 비율을 곱해주는 연산을 수행한다.
이 값은 또한 언어 모델의 다음 에측 값을 위한 입력값으로 들어가게 되는데, 언어 모델은 각 요소의 합이 1이 되는 벡터를 이용해 학습되었기 때문에 더욱 정확한 결과를 출력할 것이다.

### Language Model Top N + 나머지 평균

가우시안 분포에 가반한 GMM 확률 값과는 달리, 언어 모델은 다음에 나올 그럴듯한 글자를 여러 개 출력하고, 각 글자의 확률 값이 크게 차이가 나지 않는다. 그러나 언어 모델이 추론한 결과를 곧이 곧대로 모두 사용하기보다는, 언어모델이 추론한 상위 5개 글자의 높은 확률 값만 유지하고 나머지 글자들에 대한 확률은 그들 사이의 평균 값으로 재조정한다.
이렇게 추론 값을 조정하는 이유는 언어 모델이 높은 확률로 추론한 글자에 대한 영향력은 유지하며 동시에 만일 가우시안 분포의 확률이 높게 나온 키가 언어 모델의 예측 값에 의해서 지나치게 합산 값이 줄어들어 키보드의 구조적 특성을 무시하는 사태를 막기 위함이다.
