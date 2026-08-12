package cz.teply.scrollit;

interface IShizukuInputService {
    void destroy() = 16777114;
    boolean startTouch(float x, float y) = 1;
    boolean moveTouch(float x, float y) = 2;
    boolean finishTouch() = 3;
}
