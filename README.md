# Android application to scan distance-dependent barcodes

This repository contains a sample Android application that is able to scan the distance-aware barcodes introduced in the following research article:

- Roope Palomäki, Maria L. Montoya Freire, and Mario Di Francesco, "[Distance-Dependent Barcodes for Context-Aware Mobile Applications](https://dawcode.aalto.fi/files/mobilehci20-6.pdf)". MobileHCI 2020: *Proceedings of the 22<sup>nd</sup> International Conference on Human-Computer Interaction with Mobile Devices and Services*, Article 12, Pages 1-11, October 2020. ([Publisher's page](https://doi.org/10.1145/3379503.3403534))

See also the [related website](https://dawcode.aalto.fi/) for additional information.

The software relies on the following libraries:

- [a version of OpenCV for Android with extended support for OpenCL](https://github.com/aaltodawcode/opencv-sdk-4.0.0);

- the C++ version of [EZPWD Reed-Solomon](https://github.com/pjkundert/ezpwd-reed-solomon).

The source code is released under the [Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0) license, unless otherwise specified in the individual files (for instance, of the third-party libraries used).

### Disclaimer

This software is distributed on *"as is" basis, without warranties or conditions of any kind*. The application is a sample implementation of the distance-aware barcode scanning algorithm presented in the related research article and is not meant to be used in production. As a consequence, the code is not optimized for performance and has not been refined.

This application was tested with OnePlus 6 smartphones. Minor adjustments might be needed to make it work with other devices.
