package cz.teply.scrollit;

interface IWheelInputService {
    void destroy() = 16777114;
    boolean scroll(float verticalAxisValue, float x, float y) = 1;
}
